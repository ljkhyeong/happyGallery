#!/usr/bin/env bash
set -Eeuo pipefail
. "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[[ $# -eq 4 ]] || die "사용법: $0 <release.env> <commit SHA> <app registry digest> <frontend registry digest>"
release_env=$1
registry_revision=$2
registry_app_digest=$3
registry_frontend_digest=$4
[[ $registry_revision =~ ^[a-f0-9]{40}$ ]] || die "commit SHA가 올바르지 않습니다."
for registry_digest in "$registry_app_digest" "$registry_frontend_digest"; do
    [[ $registry_digest =~ ^sha256:[a-f0-9]{64}$ ]] || die "registry digest가 올바르지 않습니다."
done
cd "$REPO_ROOT"
[[ $(git rev-parse HEAD) == "$registry_revision" ]] || die "배포 소스와 이미지 commit이 다릅니다."
[[ -z $(git status --porcelain) ]] || die "배포 checkout에 수정 사항이 있습니다."
ruby "$SCRIPT_DIR/update-release-images.rb" --check "$release_env"

# 전송 뒤 발견할 설정 오류를 줄이기 위해 현재 복구 묶음을 먼저 확인한다.
registry_bundle=$(bash "$SCRIPT_DIR/prepare-cd-backup.sh" \
    "${CD_BACKUP_ENV:-/etc/happygallery/cd-backup.env}" \
    "${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}/../cd/backups")
export VERIFIED_RECOVERY_BUNDLE_OVERRIDE="$registry_bundle"
verify_recovery_bundle_files "$registry_bundle"
[[ -n $(find "$registry_bundle" -prune -mtime -2 -print) ]] || die "검증한 복구 묶음이 48시간보다 오래됐습니다."
[[ $(kube get nodes -o 'jsonpath={.items[0].status.nodeInfo.architecture}') == amd64 ]] || die "amd64 노드가 아닙니다."

umask 077
registry_tmp=$(mktemp -d)
trap 'rm -rf "$registry_tmp"' EXIT
registry_local_images=()
for registry_component in app frontend; do
    if [[ $registry_component == app ]]; then registry_digest=$registry_app_digest; else registry_digest=$registry_frontend_digest; fi
    registry_ref="ghcr.io/ljkhyeong/happygallery-$registry_component@$registry_digest"
    local_ref="localhost/happygallery-$registry_component:$registry_revision"
    docker pull --platform linux/amd64 "$registry_ref"
    docker image inspect "$registry_ref" > "$registry_tmp/image.json"
    ruby -rjson - "$registry_tmp/image.json" "$registry_revision" "$registry_ref" <<'RUBY'
image = JSON.parse(File.read(ARGV[0])).fetch(0)
abort 'amd64 Linux 이미지가 아닙니다.' unless image['Architecture'] == 'amd64' && image['Os'] == 'linux'
labels = image.dig('Config', 'Labels') || {}
abort '이미지 commit/source label이 다릅니다.' unless labels['org.opencontainers.image.revision'] == ARGV[1] &&
  labels['org.opencontainers.image.source'] == 'https://github.com/ljkhyeong/happyGallery'
abort '다운로드한 registry digest가 다릅니다.' unless image.fetch('RepoDigests', []).include?(ARGV[2])
RUBY
    docker tag "$registry_ref" "$local_ref"
    registry_local_images+=("$local_ref")
done

"$SCRIPT_DIR/verify-app-image.sh" "${registry_local_images[0]}"
docker save -o "$registry_tmp/images.tar" "${registry_local_images[@]}"
k3s_ctr images import "$registry_tmp/images.tar"
for local_ref in "${registry_local_images[@]}"; do
    local_digest=$(containerd_image_digest "$local_ref") || die "반입한 이미지 digest가 없습니다."
    for local_alias in "$local_ref@$local_digest" "${local_ref%:*}@$local_digest"; do
        ensure_containerd_image_alias "$local_ref" "$local_alias" "$local_digest"
    done
done

# Docker archive를 containerd로 변환하면 registry manifest와 digest가 달라질 수 있다.
# 원격 digest 검증 후 실제 반입 digest를 기존 배포 스크립트가 기록한다.
"$SCRIPT_DIR/deploy.sh" --imported "$release_env" "$registry_revision"
printf 'registry 출처: app=%s frontend=%s commit=%s\n' "$registry_app_digest" "$registry_frontend_digest" "$registry_revision"
