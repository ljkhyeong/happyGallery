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
reconciliation_token=${RESTORE_RECONCILIATION_TOKEN:-}
printf '%s' "$reconciliation_token" \
    | grep -Eq '^[0-9]{8}T[0-9]{6}Z-[a-f0-9]{64}$' \
    || die "restore-recovery-backup.sh가 발급한 RESTORE_RECONCILIATION_TOKEN을 지정해야 합니다."
[ "${CONFIRM_RESTORED_PAYMENT_RECONCILIATION:-}" = "$reconciliation_token" ] \
    || die "PG 결제 대사 완료 후 CONFIRM_RESTORED_PAYMENT_RECONCILIATION을 복원 대사 토큰으로 지정해야 합니다."
[ "${CONFIRM_RESTORED_NOTIFICATION_RECONCILIATION:-}" = "$reconciliation_token" ] \
    || die "알림 발송 대사 완료 후 CONFIRM_RESTORED_NOTIFICATION_RECONCILIATION을 복원 대사 토큰으로 지정해야 합니다."
[ "${CONFIRM_RESTORED_PRIVACY_REQUEST_RECONCILIATION:-}" = "$reconciliation_token" ] \
    || die "개인정보 요청 대사 완료 후 CONFIRM_RESTORED_PRIVACY_REQUEST_RECONCILIATION을 복원 대사 토큰으로 지정해야 합니다."

state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
restore_state_root=${HAPPYGALLERY_RESTORE_STATE_DIR:-$(dirname -- "$state_root")/restores}
pending_marker="$restore_state_root/$reconciliation_token.pending"
activation_owner="${BASHPID:-$$}-$RANDOM-$RANDOM"
activating_marker="$restore_state_root/$reconciliation_token-$activation_owner.activating"
consumed_marker="$restore_state_root/$reconciliation_token.consumed"
require_private_file "$pending_marker"
existing_activating_marker=$(find "$restore_state_root" -maxdepth 1 -type f \
    -name "$reconciliation_token*.activating" -print -quit)
[ -z "$existing_activating_marker" ] \
    || die "복원 release 활성화가 중단된 상태입니다. app 상태를 확인한 뒤 수동 복구하세요: $existing_activating_marker"
[ ! -e "$consumed_marker" ] \
    || die "이미 소비한 복원 대사 토큰입니다: $reconciliation_token"
validate_env_file "$pending_marker"
[ "$(require_env_value RESTORE_RECONCILIATION_TOKEN "$pending_marker")" = "$reconciliation_token" ] \
    || die "복원 대사 marker의 토큰이 요청과 다릅니다."
marker_created_at=$(require_env_value BACKUP_CREATED_AT "$pending_marker")
marker_metadata_sha256=$(require_env_value RECOVERY_METADATA_SHA256 "$pending_marker")
[ "$reconciliation_token" = "$marker_created_at-$marker_metadata_sha256" ] \
    || die "복원 대사 토큰이 backup timestamp와 recovery metadata checksum에 일치하지 않습니다."
[ "$(require_env_value IMAGE_TAG "$pending_marker")" = "$IMAGE_TAG" ] \
    || die "복원 대사 토큰의 release IMAGE_TAG가 요청과 다릅니다."
[ "$(require_env_value RELEASE_DIR "$pending_marker")" = "$release_dir" ] \
    || die "복원 대사 토큰의 호환 release 디렉터리가 요청과 다릅니다."

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

"$SCRIPT_DIR/prepare-restored-release-images.sh" "$release_dir"

app_ref="$APP_IMAGE@$APP_IMAGE_DIGEST"
frontend_ref="$FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST"

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -eq 0 ] \
    || die "복원 release 선택 전에 deployment/app을 0 replica로 축소해야 합니다."
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

mkdir -p "$state_root"
chmod 700 "$state_root"
kube -n "$NAMESPACE" set image deployment/app "app=$app_ref" --dry-run=server -o yaml >/dev/null
kube -n "$NAMESPACE" set image deployment/frontend "frontend=$frontend_ref" --dry-run=server -o yaml >/dev/null

activation_started=false
activation_succeeded=false
activation_cleanup_done=false
on_activation_exit() {
    status=$?
    trap - EXIT HUP INT TERM
    if [ "$activation_cleanup_done" = true ]; then
        exit "$status"
    fi
    activation_cleanup_done=true

    if { [ "$activation_started" = true ] || [ -f "$activating_marker" ]; } \
        && [ "$activation_succeeded" != true ]; then
        [ "$status" -ne 0 ] || status=1
        app_drained=false
        if kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null \
            && (wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120); then
            app_drained=true
            printf '%s\n' "오류: 복원 release 활성화 실패. app을 0 replica로 되돌리고 Pod 종료를 확인했습니다." >&2
        else
            printf '%s\n' "치명적 오류: 복원 release 활성화 실패 후 app 중지를 확인하지 못했습니다. 즉시 deployment/app 상태를 확인하세요." >&2
        fi

        if [ "$app_drained" = true ]; then
            marker_recovered=false
            if [ -f "$activating_marker" ] \
                && [ ! -e "$pending_marker" ] \
                && [ ! -e "$consumed_marker" ]; then
                mv "$activating_marker" "$pending_marker" && marker_recovered=true
            elif [ -f "$consumed_marker" ] \
                && [ ! -e "$pending_marker" ] \
                && [ ! -e "$activating_marker" ]; then
                mv "$consumed_marker" "$pending_marker" && marker_recovered=true
            elif [ -f "$pending_marker" ] \
                && [ ! -e "$activating_marker" ] \
                && [ ! -e "$consumed_marker" ]; then
                marker_recovered=true
            fi

            if [ "$marker_recovered" = true ]; then
                printf '%s\n' "복원 대사 토큰을 재시도 가능한 pending 상태로 되돌렸습니다." >&2
            else
                printf '%s\n' "치명적 오류: app은 중지했지만 복원 대사 marker를 pending 상태로 복구하지 못했습니다. marker 상태를 수동 확인하세요." >&2
            fi
        else
            printf '%s\n' "복원 대사 marker를 현재 상태로 유지합니다. app과 복원 상태를 수동 확인하세요." >&2
        fi
    fi
    exit "$status"
}

trap on_activation_exit EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM
mv "$pending_marker" "$activating_marker"
activation_started=true
info "app이 중지된 상태에서 호환 app/frontend digest를 먼저 지정합니다."
kube -n "$NAMESPACE" patch configmap app-config --type merge \
    -p "{\"data\":{\"SENTRY_RELEASE\":\"happygallery@$IMAGE_TAG\"}}" >/dev/null
kube -n "$NAMESPACE" set image deployment/app "app=$app_ref" >/dev/null
kube -n "$NAMESPACE" set image deployment/frontend "frontend=$frontend_ref" >/dev/null

configured_app_ref=$(kube -n "$NAMESPACE" get deployment app \
    -o jsonpath='{.spec.template.spec.containers[0].image}')
[ "$configured_app_ref" = "$app_ref" ] || die "호환 app digest가 Deployment에 반영되지 않았습니다."
kube -n "$NAMESPACE" rollout status deployment/frontend --timeout=3m

info "PG·알림·개인정보 요청 대사 확인과 호환 app digest 반영을 확인한 뒤 app을 1 replica로 기동합니다."
kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m

ln -sfn "$release_dir" "$state_root/current"
mv "$activating_marker" "$consumed_marker"
info "복원 release 활성화 완료. verify.sh로 전체 경로를 확인하세요."
activation_succeeded=true
