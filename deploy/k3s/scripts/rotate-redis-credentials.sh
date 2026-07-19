#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <redis-rotation.env>"
rotation_file=$1
require_command base64
validate_env_file "$rotation_file"
require_private_file "$rotation_file"
[ "${CONFIRM_REDIS_CREDENTIAL_ROTATION:-}" = rotate-happygallery-redis ] \
    || die "CONFIRM_REDIS_CREDENTIAL_ROTATION=rotate-happygallery-redis 를 지정해야 합니다."

new_password=$(require_env_value REDIS_PASSWORD "$rotation_file")
printf '%s' "$new_password" | grep -Eq '^[A-Za-z0-9._~-]{32,128}$' \
    || die "새 Redis 비밀번호는 32~128자의 영문/숫자/._~- 조합이어야 합니다."

kube -n "$NAMESPACE" get deployment app >/dev/null
kube -n "$NAMESPACE" get deployment redis >/dev/null

new_encoded=$(base64_value "$new_password")
if kube -n "$NAMESPACE" get secret happygallery-redis >/dev/null 2>&1; then
    current_encoded=$(kube -n "$NAMESPACE" get secret happygallery-redis \
        -o 'jsonpath={.data.REDIS_PASSWORD}')
    if [ "$current_encoded" = "$new_encoded" ]; then
        info "Redis Secret에는 이미 새 비밀번호가 저장되어 있습니다. 중단된 회전 절차를 재개합니다."
    fi
else
    info "Redis Secret이 없어 app을 중지한 뒤 새 비밀번호로 복구합니다."
fi

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -le 1 ] || die "단일 app replica 구성에서만 Redis 자격증명을 회전할 수 있습니다."

rotation_started=false
on_rotation_error() {
    status=$?
    trap - ERR
    if [ "$rotation_started" = true ]; then
        if kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null \
            && (wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120); then
            printf '%s\n' "오류: Redis 자격증명 회전 실패. app을 0 replica로 되돌리고 Pod 종료를 확인했습니다." >&2
        else
            printf '%s\n' "치명적 오류: Redis 자격증명 회전 실패 후 app 중지를 확인하지 못했습니다. 즉시 deployment/app 상태를 확인하세요." >&2
        fi
    fi
    exit "$status"
}

info "Redis 비밀번호 불일치를 막기 위해 app을 0 replica로 축소합니다."
rotation_started=true
trap on_rotation_error ERR
kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

if kube -n "$NAMESPACE" get secret happygallery-redis >/dev/null 2>&1; then
    printf '{"data":{"REDIS_PASSWORD":"%s"}}' "$new_encoded" \
        | kube -n "$NAMESPACE" patch secret happygallery-redis \
            --type merge --patch-file=/dev/stdin >/dev/null
else
    kube -n "$NAMESPACE" create secret generic happygallery-redis \
        --from-literal="REDIS_PASSWORD=$new_password" >/dev/null
fi

info "새 Secret을 읽도록 Redis를 재시작합니다. 기존 세션과 처리율 상태는 초기화됩니다."
kube -n "$NAMESPACE" rollout restart deployment/redis >/dev/null
kube -n "$NAMESPACE" rollout status deployment/redis --timeout=3m

if [ "${replicas:-0}" -eq 1 ]; then
    kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
    kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
fi

rotation_started=false
trap - ERR
info "Redis 자격증명 회전 완료"
