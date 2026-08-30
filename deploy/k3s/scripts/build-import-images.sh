#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

require_command docker
require_command git
require_command trivy

cd "$REPO_ROOT"

if [ -n "$(git status --porcelain)" ]; then
    die "재현 가능한 이미지 식별을 위해 작업 트리를 먼저 커밋하세요."
fi

requested_ref=${1:-HEAD}
git cat-file -e "$requested_ref^{commit}" 2>/dev/null || die "Git commit을 찾을 수 없습니다: $requested_ref"
image_tag=$(git rev-parse "$requested_ref^{commit}")
current_head=$(git rev-parse HEAD)
[ "$image_tag" = "$current_head" ] \
    || die "현재 checkout과 빌드 대상 commit이 다릅니다. 해당 commit으로 전환한 뒤 실행하세요."

: "${VITE_TOSS_CLIENT_KEY:?프런트 이미지 빌드에 VITE_TOSS_CLIENT_KEY가 필요합니다.}"

app_image="localhost/happygallery-app:$image_tag"
frontend_image="localhost/happygallery-frontend:$image_tag"
archive_dir=${IMAGE_ARCHIVE_DIR:-${TMPDIR:-/tmp}}
archive="$archive_dir/happygallery-images-$image_tag.tar"

info "백엔드 테스트와 bootJar를 빌드합니다: $image_tag"
./gradlew --no-daemon clean build

app_jar=bootstrap/build/libs/happygallery-app.jar
[ -f "$app_jar" ] || die "실행 가능한 backend jar를 찾을 수 없습니다: $app_jar"

info "백엔드 이미지를 빌드합니다: $app_image"
docker build --pull \
    --label "org.opencontainers.image.revision=$image_tag" \
    --label "org.opencontainers.image.source=happygallery" \
    --build-arg "APP_JAR=$app_jar" \
    -f deploy/k3s/images/Dockerfile.app \
    -t "$app_image" .

info "프런트 이미지를 빌드합니다: $frontend_image"
docker build --pull \
    --label "org.opencontainers.image.revision=$image_tag" \
    --label "org.opencontainers.image.source=happygallery" \
    --build-arg "VITE_TOSS_CLIENT_KEY=$VITE_TOSS_CLIENT_KEY" \
    --build-arg "VITE_SENTRY_DSN=${VITE_SENTRY_DSN:-}" \
    --build-arg "VITE_SENTRY_ENVIRONMENT=${VITE_SENTRY_ENVIRONMENT:-production}" \
    --build-arg "VITE_SENTRY_RELEASE=happygallery@$image_tag" \
    -f deploy/k3s/images/Dockerfile.frontend \
    -t "$frontend_image" .

for image in "$app_image" "$frontend_image"; do
    info "운영 반입 이미지를 검사합니다: $image"
    trivy image \
        --scanners vuln \
        --exit-code 1 \
        --exit-on-eol 1 \
        --severity HIGH,CRITICAL \
        "$image"
done

node_arch=$(kube get nodes -o jsonpath='{.items[0].status.nodeInfo.architecture}')
for image in "$app_image" "$frontend_image"; do
    image_arch=$(docker image inspect --format '{{.Architecture}}' "$image")
    [ "$image_arch" = "$node_arch" ] \
        || die "$image 아키텍처($image_arch)가 k3s 노드($node_arch)와 다릅니다."
done

mkdir -p "$archive_dir"
docker save -o "$archive" "$app_image" "$frontend_image"
info "k3s containerd에 이미지를 가져옵니다: $archive"
k3s_ctr images import "$archive"

for image in "$app_image" "$frontend_image"; do
    containerd_has_image "$image" \
        || die "k3s containerd에서 이미지를 찾을 수 없습니다: $image"
done

app_image_digest=$(containerd_image_digest "$app_image") \
    || die "백엔드 이미지 digest를 확인할 수 없습니다: $app_image"
frontend_image_digest=$(containerd_image_digest "$frontend_image") \
    || die "프런트 이미지 digest를 확인할 수 없습니다: $frontend_image"
for digest in "$app_image_digest" "$frontend_image_digest"; do
    printf '%s' "$digest" | grep -Eq '^sha256:[a-f0-9]{64}$' \
        || die "containerd가 올바르지 않은 이미지 digest를 반환했습니다: $digest"
done

for reference in \
    "$app_image@$app_image_digest" \
    "$frontend_image@$frontend_image_digest"; do
    source_image=${reference%@*}
    if ! containerd_has_image "$reference"; then
        k3s_ctr images tag "$source_image" "$reference" >/dev/null
    fi
done

cat <<EOF
이미지 import 완료
IMAGE_TAG=$image_tag
APP_IMAGE=$app_image
FRONTEND_IMAGE=$frontend_image
APP_IMAGE_DIGEST=$app_image_digest
FRONTEND_IMAGE_DIGEST=$frontend_image_digest
임시 archive=$archive
release.env에 위 다섯 값을 기록한 뒤 archive는 제거해도 됩니다.
EOF
