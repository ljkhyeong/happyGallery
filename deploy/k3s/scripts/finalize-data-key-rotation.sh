#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 0 ] || die "사용법: $0"
[ "${CONFIRM_DATA_KEY_FINALIZATION:-}" = finalize-happygallery-data-keys ] \
    || die "CONFIRM_DATA_KEY_FINALIZATION=finalize-happygallery-data-keys 를 지정해야 합니다."
require_command base64

optional_secret_value() {
    key=$1
    encoded=$(kube -n "$NAMESPACE" get secret happygallery-app -o "jsonpath={.data.$key}")
    [ -z "$encoded" ] || printf '%s' "$encoded" | base64 --decode
}

annotation() {
    name=$1
    case "$name" in
        phase) path='happygallery\.io/key-rotation-phase' ;;
        target) path='happygallery\.io/key-rotation-target-key-id' ;;
        guest-until) path='happygallery\.io/guest-previous-valid-until-epoch' ;;
        finalize-replicas) path='happygallery\.io/key-finalization-original-replicas' ;;
        *) die "알 수 없는 key rotation annotation입니다: $name" ;;
    esac
    kube -n "$NAMESPACE" get secret happygallery-app \
        -o "jsonpath={.metadata.annotations.$path}"
}

pending_social_accounts() {
    kube -n "$NAMESPACE" exec mysql-0 -- sh -ec \
        'exec mysql -N -s -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e \
          "SELECT COUNT(*) FROM user_social_accounts WHERE provider_id_enc IS NULL"'
}

kube -n "$NAMESPACE" get deployment app >/dev/null
kube -n "$NAMESPACE" get secret happygallery-app >/dev/null
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null

phase=$(annotation phase)
previous_encrypt_keys=$(optional_secret_value PREVIOUS_ENCRYPT_KEYS)
previous_hmac_keys=$(optional_secret_value PREVIOUS_HMAC_KEYS)
previous_guest_key=$(optional_secret_value GUEST_TOKEN_PREVIOUS_HMAC_SECRET)
replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -le 1 ] || die "단일 app replica 구성에서만 previous 키를 제거할 수 있습니다."

if [ "$phase" = finalized ]; then
    [ -z "$previous_encrypt_keys" ] \
        && [ -z "$previous_hmac_keys" ] \
        && [ -z "$previous_guest_key" ] \
        || die "finalized 상태인데 previous 키가 runtime Secret에 남아 있습니다."
    original_replicas=$(annotation finalize-replicas)
    case "$original_replicas" in 0|1) ;; *) die "finalize 전 app replica annotation이 올바르지 않습니다." ;; esac
    if [ "$original_replicas" -eq 1 ]; then
        kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
        kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
    fi
    info "previous 데이터/비회원 토큰 키 제거는 이미 완료됐습니다."
    exit 0
fi

resume_finalization=false
case "$phase" in
    completed)
        original_replicas=${replicas:-0}
        ;;
    finalizing)
        original_replicas=$(annotation finalize-replicas)
        case "$original_replicas" in 0|1) ;; *) die "finalize 전 app replica annotation이 올바르지 않습니다." ;; esac
        [ "${replicas:-0}" -eq 0 ] || die "중단된 finalize를 재개하려면 app을 0 replica로 유지해야 합니다."
        resume_finalization=true
        ;;
    *) die "completed 데이터 키 회전만 finalize할 수 있습니다: phase=$phase" ;;
esac
[ -n "$previous_encrypt_keys" ] \
    && [ -n "$previous_hmac_keys" ] \
    && [ -n "$previous_guest_key" ] \
    || die "finalize할 previous AES/HMAC/guest 키가 모두 필요합니다."
target_key_id=$(annotation target)
printf '%s' "$target_key_id" | grep -Eq '^[A-Za-z0-9_-]{1,32}$' \
    || die "회전 target key ID annotation이 올바르지 않습니다."
active_key_id=$(optional_secret_value FIELD_ENCRYPTION_KEY_ID)
[ "$active_key_id" = "$target_key_id" ] \
    || die "runtime active key ID가 회전 target annotation과 다릅니다."
guest_previous_valid_until=$(annotation guest-until)
printf '%s' "$guest_previous_valid_until" | grep -Eq '^[1-9][0-9]*$' \
    || die "guest previous 보존기한 annotation이 올바르지 않습니다."
now_epoch=$(date +%s)
[ "$now_epoch" -ge "$guest_previous_valid_until" ] \
    || die "기존 비회원 토큰 보존기한이 지나지 않았습니다: not-before-epoch=$guest_previous_valid_until"

pending=$(pending_social_accounts)
printf '%s' "$pending" | grep -Eq '^[0-9]+$' || die "소셜 provider ID 백필 건수를 확인할 수 없습니다."
[ "$pending" -eq 0 ] \
    || die "provider_id_enc가 없는 소셜 계정이 $pending 건 남아 previous HMAC 키를 제거할 수 없습니다."

finalization_started=false
on_finalization_error() {
    status=${1:-$?}
    trap - ERR
    trap - HUP INT TERM
    if [ "$finalization_started" = true ]; then
        if kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null \
                && (wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120); then
            printf '%s\n' "오류: previous 키 제거 실패. app을 0 replica로 유지하고 Pod 종료를 확인했습니다." >&2
        else
            printf '%s\n' "치명적 오류: previous 키 제거 실패 후 app 중지를 확인하지 못했습니다. 즉시 deployment/app 상태를 확인하세요." >&2
        fi
    fi
    exit "$status"
}


on_finalization_signal() {
    on_finalization_error 130
}

finalization_started=true
trap on_finalization_error ERR
trap on_finalization_signal HUP INT TERM
if [ "$resume_finalization" = false ]; then
    printf '{"metadata":{"annotations":{"happygallery.io/key-rotation-phase":"finalizing","happygallery.io/key-finalization-original-replicas":"%s"}}}' \
        "$original_replicas" \
        | kube -n "$NAMESPACE" patch secret happygallery-app \
            --type merge --patch-file=/dev/stdin >/dev/null
fi
info "previous 키 제거 중 쓰기를 막기 위해 app을 0 replica로 축소합니다."
kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

pending=$(pending_social_accounts)
[ "$pending" -eq 0 ] \
    || die "app 중지 후 provider_id_enc NULL 행이 발견되어 previous 키 제거를 중단합니다: $pending"

empty_encoded=$(base64_value '')
finalized_at_epoch=$(date +%s)
printf '{"metadata":{"annotations":{"happygallery.io/key-rotation-phase":"finalized","happygallery.io/key-finalization-original-replicas":"%s","happygallery.io/key-rotation-finalized-at-epoch":"%s"}},"data":{"PREVIOUS_ENCRYPT_KEYS":"%s","PREVIOUS_HMAC_KEYS":"%s","GUEST_TOKEN_PREVIOUS_HMAC_SECRET":"%s"}}' \
    "$original_replicas" "$finalized_at_epoch" "$empty_encoded" "$empty_encoded" "$empty_encoded" \
    | kube -n "$NAMESPACE" patch secret happygallery-app \
        --type merge --patch-file=/dev/stdin >/dev/null

if [ "$original_replicas" -eq 1 ]; then
    kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
    kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
fi

finalization_started=false
trap - ERR
trap - HUP INT TERM
info "previous AES/HMAC/guest 키 제거 완료. app.env도 빈 previous 값으로 갱신하세요."
