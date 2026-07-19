#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <호환 release 디렉터리>"
release_dir=$(CDPATH= cd -- "$1" 2>/dev/null && pwd) \
    || die "호환 release 디렉터리를 찾을 수 없습니다: $1"
metadata="$release_dir/metadata.env"
validate_env_file "$metadata"

APP_IMAGE=$(require_env_value APP_IMAGE "$metadata")
FRONTEND_IMAGE=$(require_env_value FRONTEND_IMAGE "$metadata")
APP_IMAGE_DIGEST=$(require_env_value APP_IMAGE_DIGEST "$metadata")
FRONTEND_IMAGE_DIGEST=$(require_env_value FRONTEND_IMAGE_DIGEST "$metadata")
IMAGE_TAG=$(require_env_value IMAGE_TAG "$metadata")

[ "${CONFIRM_RESTORED_RELEASE:-}" = "$IMAGE_TAG" ] \
    || die "CONFIRM_RESTORED_RELEASE=$IMAGE_TAG 를 지정해야 합니다."
printf '%s' "$APP_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "보존된 app 이미지 digest가 올바르지 않습니다."
printf '%s' "$FRONTEND_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "보존된 frontend 이미지 digest가 올바르지 않습니다."
printf '%s' "$IMAGE_TAG" | grep -Eq '^[A-Fa-f0-9]{12,40}$' \
    || die "보존된 IMAGE_TAG가 올바르지 않습니다."
case "$APP_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "보존된 APP_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac
case "$FRONTEND_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "보존된 FRONTEND_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac

app_ref="$APP_IMAGE@$APP_IMAGE_DIGEST"
frontend_ref="$FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST"
for reference in "$app_ref" "$frontend_ref"; do
    containerd_has_image "$reference" \
        || die "복원 release 이미지가 containerd에 남아 있지 않습니다: $reference"
done

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -eq 0 ] \
    || die "복원 release 선택 전에 deployment/app을 0 replica로 축소해야 합니다."
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
mkdir -p "$state_root"
chmod 700 "$state_root"
kube -n "$NAMESPACE" set image deployment/app "app=$app_ref" --dry-run=server -o yaml >/dev/null
kube -n "$NAMESPACE" set image deployment/frontend "frontend=$frontend_ref" --dry-run=server -o yaml >/dev/null

activation_started=false
on_activation_error() {
    status=$?
    trap - ERR
    if [ "$activation_started" = true ]; then
        if kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null \
            && (wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120); then
            printf '%s\n' "오류: 복원 release 활성화 실패. app을 0 replica로 되돌리고 Pod 종료를 확인했습니다." >&2
        else
            printf '%s\n' "치명적 오류: 복원 release 활성화 실패 후 app 중지를 확인하지 못했습니다. 즉시 deployment/app 상태를 확인하세요." >&2
        fi
    fi
    exit "$status"
}

activation_started=true
trap on_activation_error ERR
info "app이 중지된 상태에서 호환 app/frontend digest를 먼저 지정합니다."
kube -n "$NAMESPACE" patch configmap app-config --type merge \
    -p "{\"data\":{\"SENTRY_RELEASE\":\"happygallery@$IMAGE_TAG\"}}" >/dev/null
kube -n "$NAMESPACE" set image deployment/app "app=$app_ref" >/dev/null
kube -n "$NAMESPACE" set image deployment/frontend "frontend=$frontend_ref" >/dev/null

configured_app_ref=$(kube -n "$NAMESPACE" get deployment app \
    -o jsonpath='{.spec.template.spec.containers[0].image}')
[ "$configured_app_ref" = "$app_ref" ] || die "호환 app digest가 Deployment에 반영되지 않았습니다."
kube -n "$NAMESPACE" rollout status deployment/frontend --timeout=3m

info "호환 app digest가 반영된 것을 확인한 뒤 app을 1 replica로 기동합니다."
kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m

activation_started=false
trap - ERR
ln -sfn "$release_dir" "$state_root/current"
info "복원 release 활성화 완료. verify.sh로 전체 경로를 확인하세요."
