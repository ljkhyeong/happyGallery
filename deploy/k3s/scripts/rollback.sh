#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <이전 release 디렉터리>"
release_dir=$1
metadata="$release_dir/metadata.env"
validate_env_file "$metadata"

PUBLIC_HOST=$(require_env_value PUBLIC_HOST "$metadata")
APP_IMAGE=$(require_env_value APP_IMAGE "$metadata")
FRONTEND_IMAGE=$(require_env_value FRONTEND_IMAGE "$metadata")
APP_IMAGE_DIGEST=$(require_env_value APP_IMAGE_DIGEST "$metadata")
FRONTEND_IMAGE_DIGEST=$(require_env_value FRONTEND_IMAGE_DIGEST "$metadata")
IMAGE_TAG=$(require_env_value IMAGE_TAG "$metadata")

[ "${CONFIRM_ROLLBACK:-}" = "$IMAGE_TAG" ] \
    || die "CONFIRM_ROLLBACK=$IMAGE_TAG 를 지정해야 합니다."
[ "${ACKNOWLEDGE_DATABASE_NOT_ROLLED_BACK:-false}" = true ] \
    || die "DB/Flyway는 되돌아가지 않습니다. 호환성을 확인하고 ACKNOWLEDGE_DATABASE_NOT_ROLLED_BACK=true를 지정하세요."

for image_and_digest in \
    "$APP_IMAGE|$APP_IMAGE_DIGEST" \
    "$FRONTEND_IMAGE|$FRONTEND_IMAGE_DIGEST"; do
    image=${image_and_digest%%|*}
    expected_digest=${image_and_digest#*|}
    printf '%s' "$expected_digest" | grep -Eq '^sha256:[a-f0-9]{64}$' \
        || die "보존된 이미지 digest가 올바르지 않습니다: $expected_digest"
    containerd_has_image "$image@$expected_digest" \
        || die "rollback digest 이미지가 containerd에 남아 있지 않습니다: $image@$expected_digest"
done

app_ref="$APP_IMAGE@$APP_IMAGE_DIGEST"
frontend_ref="$FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST"

info "stateful/cluster 리소스는 건드리지 않고 app/frontend 이미지만 rollback합니다."
kube -n "$NAMESPACE" set image deployment/app "app=$app_ref" --dry-run=server -o yaml >/dev/null
kube -n "$NAMESPACE" set image deployment/frontend "frontend=$frontend_ref" --dry-run=server -o yaml >/dev/null
kube -n "$NAMESPACE" patch configmap app-config --type merge \
    -p "{\"data\":{\"SENTRY_RELEASE\":\"happygallery@$IMAGE_TAG\"}}" >/dev/null
kube -n "$NAMESPACE" set image deployment/app "app=$app_ref"
kube -n "$NAMESPACE" set image deployment/frontend "frontend=$frontend_ref"
kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
kube -n "$NAMESPACE" rollout status deployment/frontend --timeout=3m
"$SCRIPT_DIR/verify.sh" "$PUBLIC_HOST"

state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
ln -sfn "$release_dir" "$state_root/current"
info "app/frontend digest rollback 완료. 데이터베이스와 stateful/cluster 리소스는 변경하지 않았습니다."
