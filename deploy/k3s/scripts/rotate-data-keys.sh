#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 2 ] || die "사용법: $0 <data-key-rotation.env> <backup.env>"
rotation_file=$1
backup_file_config=$2
validate_env_file "$rotation_file"
validate_env_file "$backup_file_config"
require_private_file "$rotation_file"
require_private_file "$backup_file_config"
[ "${CONFIRM_DATA_KEY_ROTATION:-}" = rotate-happygallery-data-keys ] \
    || die "CONFIRM_DATA_KEY_ROTATION=rotate-happygallery-data-keys 를 지정해야 합니다."

for command in age base64 gzip tail tee; do
    require_command "$command"
done

for key in KUBECONFIG KUBECTL_BIN; do
    configured_value=$(env_value "$key" "$backup_file_config" 2>/dev/null || true)
    [ -z "$configured_value" ] || export "$key=$configured_value"
done

target_key_id=$(require_env_value FIELD_ENCRYPTION_KEY_ID "$rotation_file")
new_encrypt_key=$(require_env_value ENCRYPT_KEY "$rotation_file")
new_hmac_key=$(require_env_value HMAC_KEY "$rotation_file")
new_guest_key=$(require_env_value GUEST_TOKEN_HMAC_SECRET "$rotation_file")
backup_dir=$(require_env_value BACKUP_DIR "$backup_file_config")
backup_recipient=$(require_env_value BACKUP_AGE_RECIPIENT "$backup_file_config")
backup_marker=$(env_value BACKUP_TARGET_MARKER "$backup_file_config" 2>/dev/null || true)

printf '%s' "$target_key_id" | grep -Eq '^[A-Za-z0-9_-]{1,32}$' \
    || die "FIELD_ENCRYPTION_KEY_ID 형식이 올바르지 않습니다."
printf '%s' "$new_encrypt_key" | grep -Eq '^[A-Fa-f0-9]{64}$' \
    || die "새 ENCRYPT_KEY는 64자리 hex여야 합니다."
printf '%s' "$new_hmac_key" | grep -Eq '^[A-Fa-f0-9]{64}$' \
    || die "새 HMAC_KEY는 64자리 hex여야 합니다."
[ "${#new_guest_key}" -ge 32 ] || die "새 GUEST_TOKEN_HMAC_SECRET은 32자 이상이어야 합니다."
[ "$new_encrypt_key" != "$new_hmac_key" ] || die "새 AES/HMAC 키는 달라야 합니다."
[ "$new_guest_key" != "$new_encrypt_key" ] && [ "$new_guest_key" != "$new_hmac_key" ] \
    || die "새 비회원 토큰 키를 AES/HMAC 키와 재사용할 수 없습니다."
printf '%s' "$backup_recipient" | grep -Eq '^age1[0-9a-z]+$' \
    || die "BACKUP_AGE_RECIPIENT는 공백 없는 age recipient 공개키여야 합니다."
case "$backup_recipient" in
    *REPLACE*|*replace*) die "backup.env의 age recipient 예시 값을 실제 공개키로 바꾸세요." ;;
esac

backup_dir=$(CDPATH= cd -- "$backup_dir" 2>/dev/null && pwd) \
    || die "외부 백업 디렉터리를 찾을 수 없습니다: $backup_dir"
[ -n "$backup_marker" ] || backup_marker="$backup_dir/.happygallery-off-device-backup-target"
[ -f "$backup_marker" ] \
    || die "외부 백업 매체 marker가 없습니다: $backup_marker"

secret_value() {
    key=$1
    encoded=$(kube -n "$NAMESPACE" get secret happygallery-app -o "jsonpath={.data.$key}")
    [ -n "$encoded" ] || die "happygallery-app Secret에 $key 값이 없습니다."
    printf '%s' "$encoded" | base64 --decode
}

optional_secret_value() {
    key=$1
    encoded=$(kube -n "$NAMESPACE" get secret happygallery-app -o "jsonpath={.data.$key}")
    [ -z "$encoded" ] || printf '%s' "$encoded" | base64 --decode
}

effective_app_config_value() {
    key=$1
    default_value=$2
    value=$(optional_secret_value "$key")
    if [ -z "$value" ]; then
        value=$(kube -n "$NAMESPACE" get configmap app-config -o "jsonpath={.data.$key}")
    fi
    [ -n "$value" ] || value=$default_value
    printf '%s' "$value"
}

rotation_phase() {
    kube -n "$NAMESPACE" get secret happygallery-app \
        -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-phase}'
}

rotation_source_annotation() {
    kube -n "$NAMESPACE" get secret happygallery-app \
        -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-source-key-id}'
}

rotation_target_annotation() {
    kube -n "$NAMESPACE" get secret happygallery-app \
        -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-target-key-id}'
}

rotation_original_replicas_annotation() {
    kube -n "$NAMESPACE" get secret happygallery-app \
        -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-original-replicas}'
}

wait_for_rotation_job() {
    attempt=0
    while [ "$attempt" -lt 900 ]; do
        succeeded=$(kube -n "$NAMESPACE" get job happygallery-data-key-rotation \
            -o 'jsonpath={.status.succeeded}' 2>/dev/null || true)
        failed=$(kube -n "$NAMESPACE" get job happygallery-data-key-rotation \
            -o 'jsonpath={.status.failed}' 2>/dev/null || true)
        [ "${succeeded:-0}" -lt 1 ] || return 0
        if [ "${failed:-0}" -ge 1 ]; then
            kube -n "$NAMESPACE" logs job/happygallery-data-key-rotation --tail=200 >&2 || true
            return 1
        fi
        attempt=$((attempt + 1))
        sleep 1
    done
    kube -n "$NAMESPACE" logs job/happygallery-data-key-rotation --tail=200 >&2 || true
    return 1
}

cleanup_temporary_resources() {
    kube -n "$NAMESPACE" delete job happygallery-data-key-rotation \
        --ignore-not-found --wait=true >/dev/null 2>&1 || true
    kube -n "$NAMESPACE" delete secret happygallery-data-key-rotation \
        --ignore-not-found --wait=true >/dev/null 2>&1 || true
    [ -z "${rotation_env_tmp:-}" ] || rm -f "$rotation_env_tmp"
    [ -z "${backup_log_tmp:-}" ] || rm -f "$backup_log_tmp"
}

kube -n "$NAMESPACE" get deployment app >/dev/null
kube -n "$NAMESPACE" get deployment redis >/dev/null
kube -n "$NAMESPACE" get statefulset mysql >/dev/null
kube -n "$NAMESPACE" get pvc data-mysql-0 >/dev/null \
    || die "기존 MySQL PVC를 찾을 수 없습니다. 최초 Secret 생성 절차가 아닙니다."
kube -n "$NAMESPACE" get secret happygallery-app >/dev/null
kube -n "$NAMESPACE" get secret happygallery-redis >/dev/null
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null

if kube -n "$NAMESPACE" get job happygallery-data-key-rotation >/dev/null 2>&1 \
        || kube -n "$NAMESPACE" get secret happygallery-data-key-rotation >/dev/null 2>&1; then
    die "이전 key-rotation Job 또는 임시 Secret이 남아 있습니다. 실행 상태를 확인한 뒤 정리하세요."
fi

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -le 1 ] || die "단일 app replica 구성에서만 데이터 키를 회전할 수 있습니다."
app_image=$(kube -n "$NAMESPACE" get deployment app \
    -o 'jsonpath={.spec.template.spec.containers[?(@.name=="app")].image}')
printf '%s' "$app_image" | grep -Eq '@sha256:[a-f0-9]{64}$' \
    || die "현재 app Deployment 이미지가 sha256 digest로 고정되지 않았습니다."

current_key_id=$(optional_secret_value FIELD_ENCRYPTION_KEY_ID)
[ -n "$current_key_id" ] || current_key_id=v1
current_encrypt_key=$(secret_value ENCRYPT_KEY)
current_hmac_key=$(secret_value HMAC_KEY)
current_guest_key=$(secret_value GUEST_TOKEN_HMAC_SECRET)
current_previous_encrypt=$(optional_secret_value PREVIOUS_ENCRYPT_KEYS)
current_previous_hmac=$(optional_secret_value PREVIOUS_HMAC_KEYS)
current_previous_guest=$(optional_secret_value GUEST_TOKEN_PREVIOUS_HMAC_SECRET)
current_rotation_enabled=$(optional_secret_value KEY_ROTATION_ENABLED)
current_phase=$(rotation_phase)

case "$current_rotation_enabled" in
    ''|false) ;;
    *) die "runtime happygallery-app Secret의 KEY_ROTATION_ENABLED는 비어 있거나 false여야 합니다." ;;
esac

printf '%s' "$current_key_id" | grep -Eq '^[A-Za-z0-9_-]{1,32}$' \
    || die "현재 FIELD_ENCRYPTION_KEY_ID 형식이 올바르지 않습니다."
printf '%s' "$current_encrypt_key" | grep -Eq '^[A-Fa-f0-9]{64}$' \
    || die "현재 ENCRYPT_KEY 형식이 올바르지 않습니다."
printf '%s' "$current_hmac_key" | grep -Eq '^[A-Fa-f0-9]{64}$' \
    || die "현재 HMAC_KEY 형식이 올바르지 않습니다."
[ "${#current_guest_key}" -ge 32 ] || die "현재 GUEST_TOKEN_HMAC_SECRET 형식이 올바르지 않습니다."

resume_after_transition=false
resume_from_started=false
if [ "$current_key_id" = "$target_key_id" ]; then
    case "$current_phase" in
        completed)
            info "대상 키 ID의 회전은 이미 완료됐습니다. finalize 전에는 app.env의 active/previous 값을 현재 Secret과 맞추세요."
            exit 0
            ;;
        finalized)
            info "대상 키 ID의 회전과 previous 키 제거가 이미 완료됐습니다."
            exit 0
            ;;
        runtime-transitioned) ;;
        *) die "대상 키 ID가 이미 active지만 재개 가능한 회전 단계가 아닙니다." ;;
    esac
    [ "$current_encrypt_key" = "$new_encrypt_key" ] \
        && [ "$current_hmac_key" = "$new_hmac_key" ] \
        && [ "$current_guest_key" = "$new_guest_key" ] \
        || die "재개 대상 active 키가 rotation env와 다릅니다."
    source_key_id=$(rotation_source_annotation)
    [ -n "$source_key_id" ] || die "재개할 회전의 source key ID annotation이 없습니다."
    [ "$current_previous_encrypt" != "" ] \
        && [ "$current_previous_hmac" != "" ] \
        && [ "$current_previous_guest" != "" ] \
        || die "재개에 필요한 previous 키가 runtime Secret에 없습니다."
    case "$current_previous_encrypt" in
        "$source_key_id="*) ;;
        *) die "재개할 previous AES 키 ID가 source annotation과 다릅니다." ;;
    esac
    case "$current_previous_hmac" in
        "$source_key_id="*) ;;
        *) die "재개할 previous HMAC 키 ID가 source annotation과 다릅니다." ;;
    esac
    [ "$(rotation_target_annotation)" = "$target_key_id" ] \
        || die "재개할 회전의 target key ID annotation이 다릅니다."
    original_replicas=$(rotation_original_replicas_annotation)
    case "$original_replicas" in 0|1) ;; *) die "회전 전 app replica annotation이 올바르지 않습니다." ;; esac
    [ "${replicas:-0}" -eq 0 ] || die "중단된 회전을 재개하려면 app을 0 replica로 유지해야 합니다."
    wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120
    resume_after_transition=true
else
    source_key_id=$current_key_id
    [ "$source_key_id" != "$target_key_id" ] || die "source/target key ID는 달라야 합니다."
    case "$current_phase" in
        ''|finalized)
            original_replicas=${replicas:-0}
            ;;
        started)
            [ "$(rotation_source_annotation)" = "$source_key_id" ] \
                || die "중단된 회전의 source key ID가 현재 active ID와 다릅니다."
            [ "$(rotation_target_annotation)" = "$target_key_id" ] \
                || die "중단된 회전은 다른 target key ID를 사용했습니다."
            original_replicas=$(rotation_original_replicas_annotation)
            case "$original_replicas" in 0|1) ;; *) die "회전 전 app replica annotation이 올바르지 않습니다." ;; esac
            [ "${replicas:-0}" -eq 0 ] || die "중단된 회전을 재개하려면 app을 0 replica로 유지해야 합니다."
            resume_from_started=true
            ;;
        *) die "완료되지 않은 기존 데이터 키 회전 단계가 있습니다: $current_phase" ;;
    esac
    [ -z "$current_previous_encrypt" ] \
        && [ -z "$current_previous_hmac" ] \
        && [ -z "$current_previous_guest" ] \
        || die "previous 키를 finalize한 뒤 다음 회전을 시작하세요."
    [ "$current_encrypt_key" != "$new_encrypt_key" ] \
        && [ "$current_hmac_key" != "$new_hmac_key" ] \
        && [ "$current_guest_key" != "$new_guest_key" ] \
        || die "새 키는 현재 active 키와 모두 달라야 합니다."
    for new_key in "$new_encrypt_key" "$new_hmac_key" "$new_guest_key"; do
        for old_key in "$current_encrypt_key" "$current_hmac_key" "$current_guest_key"; do
            [ "$new_key" != "$old_key" ] || die "신규 키를 기존 AES/HMAC/guest 용도와 재사용할 수 없습니다."
        done
    done
fi

rotation_started=false
rotation_env_tmp=
backup_log_tmp=
on_rotation_error() {
    status=${1:-$?}
    trap - ERR
    trap - HUP INT TERM
    cleanup_temporary_resources
    if [ "$rotation_started" = true ]; then
        if kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null \
                && (wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120); then
            printf '%s\n' "오류: 데이터 키 회전 실패. app을 0 replica로 유지하고 Pod 종료를 확인했습니다." >&2
        else
            printf '%s\n' "치명적 오류: 데이터 키 회전 실패 후 app 중지를 확인하지 못했습니다. 즉시 deployment/app 상태를 확인하세요." >&2
        fi
    fi
    exit "$status"
}

on_rotation_signal() {
    on_rotation_error 130
}

rotation_started=true
trap on_rotation_error ERR
trap on_rotation_signal HUP INT TERM

if [ "$resume_after_transition" = false ]; then
    if [ "$resume_from_started" = false ]; then
        printf '{"metadata":{"annotations":{"happygallery.io/key-rotation-phase":"started","happygallery.io/key-rotation-source-key-id":"%s","happygallery.io/key-rotation-target-key-id":"%s","happygallery.io/key-rotation-original-replicas":"%s"}}}' \
            "$source_key_id" "$target_key_id" "$original_replicas" \
            | kube -n "$NAMESPACE" patch secret happygallery-app \
                --type merge --patch-file=/dev/stdin >/dev/null
    fi
    info "키 회전 중 쓰기를 막기 위해 app을 0 replica로 축소합니다."
    kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null
    wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

    info "app Pod 종료 후 fresh off-device 암호화 백업을 생성합니다."
    umask 077
    backup_log_tmp=$(mktemp "${TMPDIR:-/tmp}/happygallery-data-key-backup.XXXXXX")
    BACKUP_DIR="$backup_dir" \
        BACKUP_AGE_RECIPIENT="$backup_recipient" \
        BACKUP_TARGET_MARKER="$backup_marker" \
        "$SCRIPT_DIR/backup-mysql.sh" | tee "$backup_log_tmp"
    fresh_backup=$(tail -n 1 "$backup_log_tmp")
    rm -f "$backup_log_tmp"
    backup_log_tmp=
    case "$fresh_backup" in
        "$backup_dir"/happygallery-*.sql.gz.age) ;;
        *) die "fresh backup 결과 경로가 외부 백업 디렉터리와 일치하지 않습니다: $fresh_backup" ;;
    esac
    verify_checksum "$fresh_backup"

    rotation_env_tmp=$(mktemp "${TMPDIR:-/tmp}/happygallery-data-key-rotation.XXXXXX")
    cat > "$rotation_env_tmp" <<EOF
FIELD_ENCRYPTION_KEY_ID=$target_key_id
ENCRYPT_KEY=$new_encrypt_key
HMAC_KEY=$new_hmac_key
PREVIOUS_ENCRYPT_KEYS=$source_key_id=$current_encrypt_key
PREVIOUS_HMAC_KEYS=$source_key_id=$current_hmac_key
GUEST_TOKEN_HMAC_SECRET=$new_guest_key
GUEST_TOKEN_PREVIOUS_HMAC_SECRET=$current_guest_key
KEY_ROTATION_ENABLED=true
KEY_ROTATION_SOURCE_KEY_ID=$source_key_id
EOF
    kube -n "$NAMESPACE" create secret generic happygallery-data-key-rotation \
        --from-env-file="$rotation_env_tmp" --dry-run=client -o yaml \
        | kube apply -f - >/dev/null
    rm -f "$rotation_env_tmp"
    rotation_env_tmp=

    info "현재 app digest로 외부 Service가 없는 데이터 키 회전 Job을 실행합니다."
    kube apply -f - >/dev/null <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: happygallery-data-key-rotation
  namespace: $NAMESPACE
  labels:
    app.kubernetes.io/name: key-rotation
    app.kubernetes.io/part-of: happygallery
spec:
  backoffLimit: 0
  activeDeadlineSeconds: 900
  template:
    metadata:
      labels:
        app.kubernetes.io/name: key-rotation
        app.kubernetes.io/part-of: happygallery
    spec:
      automountServiceAccountToken: false
      restartPolicy: Never
      securityContext:
        runAsNonRoot: true
        runAsUser: 10001
        runAsGroup: 10001
        fsGroup: 10001
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: key-rotation
          image: $app_image
          imagePullPolicy: Never
          envFrom:
            - configMapRef:
                name: app-config
            - secretRef:
                name: happygallery-app
          env:
            - name: SERVER_PORT
              value: "0"
            - name: MANAGEMENT_PORT
              value: "0"
            - name: FIELD_ENCRYPTION_KEY_ID
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: FIELD_ENCRYPTION_KEY_ID } }
            - name: ENCRYPT_KEY
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: ENCRYPT_KEY } }
            - name: HMAC_KEY
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: HMAC_KEY } }
            - name: PREVIOUS_ENCRYPT_KEYS
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: PREVIOUS_ENCRYPT_KEYS } }
            - name: PREVIOUS_HMAC_KEYS
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: PREVIOUS_HMAC_KEYS } }
            - name: GUEST_TOKEN_HMAC_SECRET
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: GUEST_TOKEN_HMAC_SECRET } }
            - name: GUEST_TOKEN_PREVIOUS_HMAC_SECRET
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: GUEST_TOKEN_PREVIOUS_HMAC_SECRET } }
            - name: KEY_ROTATION_ENABLED
              value: "true"
            - name: KEY_ROTATION_SOURCE_KEY_ID
              valueFrom: { secretKeyRef: { name: happygallery-data-key-rotation, key: KEY_ROTATION_SOURCE_KEY_ID } }
            - name: SPRING_DATA_REDIS_PASSWORD
              valueFrom: { secretKeyRef: { name: happygallery-redis, key: REDIS_PASSWORD } }
            - name: JAVA_TOOL_OPTIONS
              value: -XX:MaxRAMPercentage=75 -Djava.io.tmpdir=/tmp
          resources:
            requests: { cpu: 250m, memory: 512Mi }
            limits: { cpu: "1", memory: 1Gi }
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities: { drop: ["ALL"] }
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: { sizeLimit: 256Mi }
EOF
    wait_for_rotation_job
    kube -n "$NAMESPACE" logs job/happygallery-data-key-rotation --tail=200
    cleanup_temporary_resources

    guest_expiry_hours=$(effective_app_config_value GUEST_TOKEN_EXPIRY_HOURS 720)
    recovery_expiry_hours=$(effective_app_config_value GUEST_TOKEN_RECOVERY_EXPIRY_HOURS 24)
    for key_and_value in \
        "GUEST_TOKEN_EXPIRY_HOURS|$guest_expiry_hours" \
        "GUEST_TOKEN_RECOVERY_EXPIRY_HOURS|$recovery_expiry_hours"; do
        key=${key_and_value%%|*}
        value=${key_and_value#*|}
        printf '%s' "$value" | grep -Eq '^[1-9][0-9]{0,3}$' \
            || die "$key 값은 1~9999 범위의 정수여야 합니다."
    done
    guest_previous_ttl_hours=$guest_expiry_hours
    for value in "$guest_expiry_hours" "$recovery_expiry_hours"; do
        [ "$value" -le "$guest_previous_ttl_hours" ] || guest_previous_ttl_hours=$value
    done
    transitioned_at_epoch=$(date +%s)
    guest_previous_valid_until_epoch=$((transitioned_at_epoch + (guest_previous_ttl_hours + 1) * 3600))
    backup_name=$(basename -- "$fresh_backup")

    info "Job 성공 후 runtime Secret을 새 active/구 previous 키로 전환합니다."
    new_key_id_encoded=$(base64_value "$target_key_id")
    new_encrypt_encoded=$(base64_value "$new_encrypt_key")
    new_hmac_encoded=$(base64_value "$new_hmac_key")
    previous_encrypt_encoded=$(base64_value "$source_key_id=$current_encrypt_key")
    previous_hmac_encoded=$(base64_value "$source_key_id=$current_hmac_key")
    new_guest_encoded=$(base64_value "$new_guest_key")
    previous_guest_encoded=$(base64_value "$current_guest_key")
    printf '{"metadata":{"annotations":{"happygallery.io/key-rotation-phase":"runtime-transitioned","happygallery.io/key-rotation-source-key-id":"%s","happygallery.io/key-rotation-target-key-id":"%s","happygallery.io/key-rotation-original-replicas":"%s","happygallery.io/key-rotation-backup":"%s","happygallery.io/guest-previous-valid-until-epoch":"%s","happygallery.io/guest-proof-previous-issued-before-epoch":"%s"}},"data":{"FIELD_ENCRYPTION_KEY_ID":"%s","ENCRYPT_KEY":"%s","HMAC_KEY":"%s","PREVIOUS_ENCRYPT_KEYS":"%s","PREVIOUS_HMAC_KEYS":"%s","GUEST_TOKEN_HMAC_SECRET":"%s","GUEST_TOKEN_PREVIOUS_HMAC_SECRET":"%s"}}' \
        "$source_key_id" "$target_key_id" "$original_replicas" "$backup_name" \
        "$guest_previous_valid_until_epoch" "$transitioned_at_epoch" \
        "$new_key_id_encoded" "$new_encrypt_encoded" \
        "$new_hmac_encoded" "$previous_encrypt_encoded" "$previous_hmac_encoded" \
        "$new_guest_encoded" "$previous_guest_encoded" \
        | kube -n "$NAMESPACE" patch secret happygallery-app \
            --type merge --patch-file=/dev/stdin >/dev/null
fi

info "구 AES/HMAC에 결합된 관리자 세션과 처리율 상태를 Redis에서 제거합니다."
kube -n "$NAMESPACE" exec deployment/redis -- sh -ec \
    'REDISCLI_AUTH="$REDIS_PASSWORD" exec redis-cli FLUSHALL' >/dev/null

if [ "$original_replicas" -eq 1 ]; then
    info "새 active/previous 키링으로 app을 재기동합니다."
    kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
    kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
fi

completed_at_epoch=$(date +%s)
printf '{"metadata":{"annotations":{"happygallery.io/key-rotation-phase":"completed","happygallery.io/key-rotation-completed-at-epoch":"%s"}}}' \
    "$completed_at_epoch" \
    | kube -n "$NAMESPACE" patch secret happygallery-app \
        --type merge --patch-file=/dev/stdin >/dev/null

rotation_started=false
trap - ERR
trap - HUP INT TERM
info "데이터 키 회전 완료. app.env와 off-device recovery bundle에 active/previous 키 상태를 반영하세요."
info "guest 유예기간과 소셜 provider ID 백필 완료 후 finalize-data-key-rotation.sh를 실행하세요."
