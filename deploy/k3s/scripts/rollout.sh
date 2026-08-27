#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <release.env>"
release_env=$1
validate_env_file "$release_env"

PUBLIC_HOST=$(require_env_value PUBLIC_HOST "$release_env")
ACME_EMAIL=$(require_env_value ACME_EMAIL "$release_env")
APP_IMAGE=$(require_env_value APP_IMAGE "$release_env")
FRONTEND_IMAGE=$(require_env_value FRONTEND_IMAGE "$release_env")
APP_IMAGE_DIGEST=$(require_env_value APP_IMAGE_DIGEST "$release_env")
FRONTEND_IMAGE_DIGEST=$(require_env_value FRONTEND_IMAGE_DIGEST "$release_env")
IMAGE_TAG=$(require_env_value IMAGE_TAG "$release_env")
GOOGLE_OAUTH_REDIRECT_URI=$(require_env_value GOOGLE_OAUTH_REDIRECT_URI "$release_env")
NAVER_OAUTH_REDIRECT_URI=$(require_env_value NAVER_OAUTH_REDIRECT_URI "$release_env")
KAKAO_OAUTH_REDIRECT_URI=$(require_env_value KAKAO_OAUTH_REDIRECT_URI "$release_env")
export PUBLIC_HOST ACME_EMAIL APP_IMAGE FRONTEND_IMAGE APP_IMAGE_DIGEST FRONTEND_IMAGE_DIGEST IMAGE_TAG

[ "$GOOGLE_OAUTH_REDIRECT_URI" = "https://$PUBLIC_HOST/api/v1/auth/social/callback/google" ] \
    || die "GOOGLE_OAUTH_REDIRECT_URI가 공개 host의 exact callback과 다릅니다."
[ "$NAVER_OAUTH_REDIRECT_URI" = "https://$PUBLIC_HOST/api/v1/auth/social/callback/naver" ] \
    || die "NAVER_OAUTH_REDIRECT_URI가 공개 host의 exact callback과 다릅니다."
[ "$KAKAO_OAUTH_REDIRECT_URI" = "https://$PUBLIC_HOST/api/v1/auth/social/callback/kakao" ] \
    || die "KAKAO_OAUTH_REDIRECT_URI가 공개 host의 exact callback과 다릅니다."

case "$APP_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "APP_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac
case "$FRONTEND_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "FRONTEND_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac
printf '%s' "$APP_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "APP_IMAGE_DIGEST는 sha256 digest여야 합니다."
printf '%s' "$FRONTEND_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "FRONTEND_IMAGE_DIGEST는 sha256 digest여야 합니다."

kube get crd certificates.cert-manager.io >/dev/null \
    || die "cert-manager가 설치되지 않았습니다. bootstrap-cluster.sh를 먼저 실행하세요."
kube get crd middlewares.traefik.io >/dev/null \
    || die "Traefik Middleware CRD를 찾을 수 없습니다."

for secret in happygallery-mysql happygallery-redis happygallery-app happygallery-alertmanager; do
    kube -n "$NAMESPACE" get secret "$secret" >/dev/null \
        || die "runtime Secret이 없습니다: $secret"
done

for image_and_digest in \
    "$APP_IMAGE|$APP_IMAGE_DIGEST" \
    "$FRONTEND_IMAGE|$FRONTEND_IMAGE_DIGEST"; do
    image=${image_and_digest%%|*}
    expected_digest=${image_and_digest#*|}
    containerd_has_image "$image" \
        || die "k3s containerd에 import되지 않은 이미지입니다: $image"
    actual_digest=$(containerd_image_digest "$image") \
        || die "k3s containerd 이미지 digest를 확인할 수 없습니다: $image"
    [ "$actual_digest" = "$expected_digest" ] \
        || die "$image digest가 release.env와 다릅니다: expected=$expected_digest actual=$actual_digest"
    containerd_has_image "$image@$expected_digest" \
        || die "digest 고정 이미지 참조가 containerd에 없습니다: $image@$expected_digest"
done

if kube -n "$NAMESPACE" get statefulset mysql >/dev/null 2>&1; then
    recovery_bundle=$(require_env_value VERIFIED_RECOVERY_BUNDLE "$release_env")
    verify_recovery_bundle_files "$recovery_bundle"
    recent=$(find "$recovery_bundle" -prune -mtime -2 -print)
    [ -n "$recent" ] || die "배포 전 백업이 48시간보다 오래됐습니다. 새 백업을 만드세요."
    info "배포 전 DB·미디어·호환 release 복구 묶음과 생성 시각을 확인했습니다."
fi

state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
timestamp=$(date -u '+%Y%m%dT%H%M%SZ')
release_dir="$state_root/$timestamp-$IMAGE_TAG"
mkdir -p "$release_dir"
chmod 700 "$state_root" "$release_dir"
manifest="$release_dir/manifests.yaml"

"$SCRIPT_DIR/render-manifests.sh" "$manifest"
cat > "$release_dir/metadata.env" <<EOF
PUBLIC_HOST=$PUBLIC_HOST
ACME_EMAIL=$ACME_EMAIL
APP_IMAGE=$APP_IMAGE
FRONTEND_IMAGE=$FRONTEND_IMAGE
APP_IMAGE_DIGEST=$APP_IMAGE_DIGEST
FRONTEND_IMAGE_DIGEST=$FRONTEND_IMAGE_DIGEST
IMAGE_TAG=$IMAGE_TAG
GOOGLE_OAUTH_REDIRECT_URI=$GOOGLE_OAUTH_REDIRECT_URI
NAVER_OAUTH_REDIRECT_URI=$NAVER_OAUTH_REDIRECT_URI
KAKAO_OAUTH_REDIRECT_URI=$KAKAO_OAUTH_REDIRECT_URI
APPLIED_AT=$timestamp
EOF
chmod 600 "$release_dir/metadata.env" "$manifest"

info "Kubernetes API에서 server-side dry-run을 실행합니다."
kube apply --dry-run=server -f "$manifest" >/dev/null

info "release manifest를 적용합니다: $release_dir"
kube apply -f "$manifest"
kube -n "$NAMESPACE" rollout restart deployment/prometheus >/dev/null
kube -n "$NAMESPACE" rollout restart deployment/alertmanager >/dev/null
kube -n "$NAMESPACE" rollout restart deployment/grafana >/dev/null
kube -n "$NAMESPACE" rollout status statefulset/mysql --timeout=5m
kube -n "$NAMESPACE" rollout status deployment/redis --timeout=3m
kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
kube -n "$NAMESPACE" rollout status deployment/frontend --timeout=3m
kube -n "$NAMESPACE" rollout status deployment/prometheus --timeout=3m
kube -n "$NAMESPACE" rollout status deployment/alertmanager --timeout=3m
kube -n "$NAMESPACE" rollout status deployment/grafana --timeout=3m
kube -n "$NAMESPACE" wait --for=condition=Ready certificate/happygallery-tls --timeout=5m

"$SCRIPT_DIR/verify.sh" "$PUBLIC_HOST"
ln -sfn "$release_dir" "$state_root/current"
info "rollout 완료: $release_dir"
