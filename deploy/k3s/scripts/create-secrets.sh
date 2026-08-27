#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 4 ] || die "사용법: $0 <mysql.env> <redis.env> <app.env> <alert-webhook-url-file>"
mysql_file=$1
redis_file=$2
app_file=$3
alert_webhook_file=$4
require_command base64

for file in "$mysql_file" "$redis_file" "$app_file"; do
    validate_env_file "$file"
    require_private_file "$file"
done

validate_allowed_env_keys() {
    local file=$1
    local label=$2
    shift 2
    local line trimmed key allowed_key matched

    while IFS= read -r line || [ -n "$line" ]; do
        trimmed=${line#"${line%%[![:space:]]*}"}
        case "$trimmed" in
            ''|\#*) continue ;;
        esac

        key=${line%%=*}
        matched=false
        for allowed_key in "$@"; do
            if [ "$key" = "$allowed_key" ]; then
                matched=true
                break
            fi
        done
        [ "$matched" = true ] \
            || die "$label 환경 파일에 허용되지 않은 키가 있습니다: $key"
    done < "$file"
}

validate_allowed_env_keys "$mysql_file" MySQL \
    MYSQL_ROOT_PASSWORD MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD
validate_allowed_env_keys "$redis_file" Redis \
    REDIS_PASSWORD
validate_allowed_env_keys "$app_file" 애플리케이션 \
    DB_USERNAME DB_PASSWORD \
    FIELD_ENCRYPTION_KEY_ID ENCRYPT_KEY HMAC_KEY \
    PREVIOUS_ENCRYPT_KEYS PREVIOUS_HMAC_KEYS \
    GUEST_TOKEN_HMAC_SECRET GUEST_TOKEN_PREVIOUS_HMAC_SECRET \
    TOSS_SECRET_KEY \
    PAYMENT_TIMEOUT_MILLIS \
    TOSS_TIMEOUT_MILLIS TOSS_CONNECT_TIMEOUT_MILLIS TOSS_ACQUIRE_TIMEOUT_MILLIS \
    DELIVERY_TRACKING_ENABLED \
    DELIVERY_API_KEY DELIVERY_API_SECRET_KEY \
    DELIVERY_WEBHOOK_ENDPOINT_ID DELIVERY_WEBHOOK_SECRET \
    DELIVERY_API_BASE_URL \
    DELIVERY_API_TIMEOUT_MILLIS DELIVERY_API_CONNECT_TIMEOUT_MILLIS \
    DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS DELIVERY_API_MAX_CONNECTIONS \
    DELIVERY_API_KEEP_ALIVE_MILLIS \
    GOOGLE_OAUTH_CLIENT_ID GOOGLE_OAUTH_CLIENT_SECRET \
    NAVER_OAUTH_CLIENT_ID NAVER_OAUTH_CLIENT_SECRET \
    ALIMTALK_APP_KEY ALIMTALK_SECRET_KEY ALIMTALK_SENDER_KEY \
    SMS_API_KEY SMS_API_SECRET SMS_SENDER_NUMBER \
    EMAIL_VERIFICATION_SMTP_HOST EMAIL_VERIFICATION_SMTP_PORT \
    EMAIL_VERIFICATION_SMTP_USERNAME EMAIL_VERIFICATION_SMTP_PASSWORD \
    EMAIL_VERIFICATION_FROM \
    ORDER_SHIPPING_FEE \
    NOTIFICATION_TIMEOUT_MILLIS \
    ALIMTALK_TIMEOUT_MILLIS ALIMTALK_CONNECT_TIMEOUT_MILLIS \
    ALIMTALK_ACQUIRE_TIMEOUT_MILLIS \
    SMS_TIMEOUT_MILLIS SMS_CONNECT_TIMEOUT_MILLIS SMS_ACQUIRE_TIMEOUT_MILLIS \
    EMAIL_VERIFICATION_TIMEOUT_MILLIS \
    EMAIL_VERIFICATION_CONNECTION_TIMEOUT_MILLIS \
    EMAIL_VERIFICATION_READ_TIMEOUT_MILLIS \
    EMAIL_VERIFICATION_WRITE_TIMEOUT_MILLIS \
    EMAIL_VERIFICATION_STARTTLS_ENABLED EMAIL_VERIFICATION_SSL_ENABLED \
    EMAIL_VERIFICATION_EXECUTOR_POOL_SIZE \
    EMAIL_VERIFICATION_EXECUTOR_QUEUE_CAPACITY \
    SENTRY_DSN

require_private_file "$alert_webhook_file"
alert_webhook_lines=$(awk 'NF { count++ } END { print count + 0 }' "$alert_webhook_file")
[ "$alert_webhook_lines" -eq 1 ] || die "Alertmanager webhook URL 파일에는 URL 한 줄만 있어야 합니다."
grep -Eq '^https://[^[:space:]]+$' "$alert_webhook_file" \
    || die "Alertmanager webhook URL은 공백 없는 https URL이어야 합니다."
grep -q 'example\.com' "$alert_webhook_file" \
    && die "Alertmanager 예시 URL을 실제 외부 수신 URL로 바꾸세요."

for key in MYSQL_ROOT_PASSWORD MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD; do
    require_env_value "$key" "$mysql_file" >/dev/null
done
for key in REDIS_PASSWORD; do
    require_env_value "$key" "$redis_file" >/dev/null
done
for key in \
    DB_USERNAME DB_PASSWORD ENCRYPT_KEY HMAC_KEY GUEST_TOKEN_HMAC_SECRET \
    FIELD_ENCRYPTION_KEY_ID \
    TOSS_SECRET_KEY GOOGLE_OAUTH_CLIENT_ID GOOGLE_OAUTH_CLIENT_SECRET \
    NAVER_OAUTH_CLIENT_ID NAVER_OAUTH_CLIENT_SECRET \
    ALIMTALK_APP_KEY ALIMTALK_SECRET_KEY ALIMTALK_SENDER_KEY \
    SMS_API_KEY SMS_API_SECRET SMS_SENDER_NUMBER \
    EMAIL_VERIFICATION_SMTP_HOST EMAIL_VERIFICATION_SMTP_USERNAME \
    EMAIL_VERIFICATION_SMTP_PASSWORD EMAIL_VERIFICATION_FROM; do
    require_env_value "$key" "$app_file" >/dev/null
done

mysql_database=$(require_env_value MYSQL_DATABASE "$mysql_file")
mysql_root_password=$(require_env_value MYSQL_ROOT_PASSWORD "$mysql_file")
mysql_user=$(require_env_value MYSQL_USER "$mysql_file")
mysql_password=$(require_env_value MYSQL_PASSWORD "$mysql_file")
db_user=$(require_env_value DB_USERNAME "$app_file")
db_password=$(require_env_value DB_PASSWORD "$app_file")
redis_password=$(require_env_value REDIS_PASSWORD "$redis_file")
encrypt_key=$(require_env_value ENCRYPT_KEY "$app_file")
hmac_key=$(require_env_value HMAC_KEY "$app_file")
guest_token_key=$(require_env_value GUEST_TOKEN_HMAC_SECRET "$app_file")
field_key_id=$(require_env_value FIELD_ENCRYPTION_KEY_ID "$app_file")
previous_encrypt_keys=$(env_value PREVIOUS_ENCRYPT_KEYS "$app_file" 2>/dev/null || true)
previous_hmac_keys=$(env_value PREVIOUS_HMAC_KEYS "$app_file" 2>/dev/null || true)
previous_guest_token_key=$(env_value GUEST_TOKEN_PREVIOUS_HMAC_SECRET "$app_file" 2>/dev/null || true)

[ "$mysql_database" = happygallery ] || die "MYSQL_DATABASE는 manifest의 happygallery와 같아야 합니다."
[ "$mysql_user" = "$db_user" ] || die "MYSQL_USER와 DB_USERNAME이 다릅니다."
[ "$mysql_password" = "$db_password" ] || die "MYSQL_PASSWORD와 DB_PASSWORD가 다릅니다."
[ "${#mysql_password}" -ge 24 ] || die "MySQL 애플리케이션 비밀번호는 24자 이상이어야 합니다."
[ "${#mysql_root_password}" -ge 24 ] || die "MySQL root 비밀번호는 24자 이상이어야 합니다."
[ "${#redis_password}" -ge 32 ] || die "Redis 비밀번호는 32자 이상이어야 합니다."
[ "${#guest_token_key}" -ge 32 ] || die "GUEST_TOKEN_HMAC_SECRET은 32자 이상이어야 합니다."
printf '%s' "$encrypt_key" | grep -Eq '^[A-Fa-f0-9]{64}$' || die "ENCRYPT_KEY는 64자리 hex여야 합니다."
printf '%s' "$hmac_key" | grep -Eq '^[A-Fa-f0-9]{64}$' || die "HMAC_KEY는 64자리 hex여야 합니다."
printf '%s' "$field_key_id" | grep -Eq '^[A-Za-z0-9_-]{1,32}$' \
    || die "FIELD_ENCRYPTION_KEY_ID 형식이 올바르지 않습니다."
[ "$encrypt_key" != "$hmac_key" ] || die "ENCRYPT_KEY와 HMAC_KEY는 달라야 합니다."
[ "$guest_token_key" != "$encrypt_key" ] && [ "$guest_token_key" != "$hmac_key" ] \
    || die "비회원 토큰 키를 AES/HMAC 키와 재사용할 수 없습니다."

validate_previous_keyring() {
    local label=$1
    local keyring=$2
    local old_ifs entry key_id key_value seen
    [ -n "$keyring" ] || return 0
    old_ifs=$IFS
    IFS=,
    seen=,
    for entry in $keyring; do
        key_id=${entry%%=*}
        key_value=${entry#*=}
        [ "$entry" != "$key_id" ] || die "$label 이전 키 형식은 keyId=64자리hex 목록이어야 합니다."
        printf '%s' "$key_id" | grep -Eq '^[A-Za-z0-9_-]{1,32}$' \
            || die "$label 이전 키 ID 형식이 올바르지 않습니다."
        [ "$key_id" != "$field_key_id" ] || die "$label 이전 키 ID가 active ID와 같습니다."
        case "$seen" in
            *,"$key_id",*) die "$label 이전 키 ID가 중복되었습니다: $key_id" ;;
        esac
        seen="$seen$key_id,"
        printf '%s' "$key_value" | grep -Eq '^[A-Fa-f0-9]{64}$' \
            || die "$label 이전 키는 64자리 hex여야 합니다."
    done
    IFS=$old_ifs
}

validate_previous_keyring "AES" "$previous_encrypt_keys"
validate_previous_keyring "HMAC" "$previous_hmac_keys"
if [ -n "$previous_guest_token_key" ]; then
    [ "${#previous_guest_token_key}" -ge 32 ] \
        || die "GUEST_TOKEN_PREVIOUS_HMAC_SECRET은 비어 있거나 32자 이상이어야 합니다."
    [ "$previous_guest_token_key" != "$guest_token_key" ] \
        || die "활성/이전 비회원 토큰 키는 달라야 합니다."
fi

kube apply -f "$BASE_DIR/namespace.yaml" >/dev/null

if kube -n "$NAMESPACE" get secret happygallery-app >/dev/null 2>&1; then
    guard_data_key_change() {
        local key=$1
        local desired_value=$2
        local current_encoded desired_encoded
        current_encoded=$(kube -n "$NAMESPACE" get secret happygallery-app \
            -o "jsonpath={.data.$key}")
        desired_encoded=$(base64_value "$desired_value")
        if [ "$key" = FIELD_ENCRYPTION_KEY_ID ] \
                && [ -z "$current_encoded" ] \
                && [ "$desired_value" = v1 ]; then
            return 0
        fi
        [ "$current_encoded" = "$desired_encoded" ] \
            || die "$key 변경은 rotate-data-keys.sh/finalize-data-key-rotation.sh 절차로만 수행하세요."
    }
    guard_data_key_change FIELD_ENCRYPTION_KEY_ID "$field_key_id"
    guard_data_key_change ENCRYPT_KEY "$encrypt_key"
    guard_data_key_change HMAC_KEY "$hmac_key"
    guard_data_key_change PREVIOUS_ENCRYPT_KEYS "$previous_encrypt_keys"
    guard_data_key_change PREVIOUS_HMAC_KEYS "$previous_hmac_keys"
    guard_data_key_change GUEST_TOKEN_HMAC_SECRET "$guest_token_key"
    guard_data_key_change GUEST_TOKEN_PREVIOUS_HMAC_SECRET "$previous_guest_token_key"
fi

if kube -n "$NAMESPACE" get secret happygallery-redis >/dev/null 2>&1; then
    current_redis_encoded=$(kube -n "$NAMESPACE" get secret happygallery-redis \
        -o 'jsonpath={.data.REDIS_PASSWORD}')
    desired_redis_encoded=$(base64_value "$redis_password")
    [ "$current_redis_encoded" = "$desired_redis_encoded" ] \
        || die "REDIS_PASSWORD는 일반 Secret 교체로 변경할 수 없습니다. rotate-redis-credentials.sh 절차를 사용하세요."
elif kube -n "$NAMESPACE" get deployment redis >/dev/null 2>&1; then
    die "기존 Redis deployment의 Secret이 없습니다. rotate-redis-credentials.sh로 app을 중지한 뒤 Secret과 Redis를 함께 복구하세요."
fi

if kube -n "$NAMESPACE" get pvc data-mysql-0 >/dev/null 2>&1; then
    kube -n "$NAMESPACE" get secret happygallery-app >/dev/null 2>&1 \
        || die "기존 MySQL PVC가 있지만 데이터 결합 키가 든 happygallery-app Secret이 없습니다. 분리 보관한 기존 Secret을 먼저 복구하세요."
    kube -n "$NAMESPACE" get secret happygallery-mysql >/dev/null 2>&1 \
        || die "기존 MySQL PVC가 있지만 happygallery-mysql Secret이 없습니다. 수동 복구가 필요합니다."

    for key_and_value in \
        "MYSQL_ROOT_PASSWORD|$mysql_root_password" \
        "MYSQL_DATABASE|$mysql_database" \
        "MYSQL_USER|$mysql_user" \
        "MYSQL_PASSWORD|$mysql_password"; do
        key=${key_and_value%%|*}
        desired_value=${key_and_value#*|}
        current_encoded=$(kube -n "$NAMESPACE" get secret happygallery-mysql \
            -o "jsonpath={.data.$key}")
        desired_encoded=$(base64_value "$desired_value")
        [ "$current_encoded" = "$desired_encoded" ] \
            || die "기존 MySQL PVC에서는 $key 값을 Secret만 바꿀 수 없습니다. rotate-mysql-credentials.sh 절차를 사용하세요."
    done
fi

kube create secret generic happygallery-mysql \
    --namespace "$NAMESPACE" \
    --from-env-file="$mysql_file" \
    --dry-run=client -o yaml | kube apply -f - >/dev/null

kube create secret generic happygallery-redis \
    --namespace "$NAMESPACE" \
    --from-env-file="$redis_file" \
    --dry-run=client -o yaml | kube apply -f - >/dev/null

kube create secret generic happygallery-app \
    --namespace "$NAMESPACE" \
    --from-env-file="$app_file" \
    --dry-run=client -o yaml | kube apply -f - >/dev/null

kube create secret generic happygallery-alertmanager \
    --namespace "$NAMESPACE" \
    --from-file=webhook-url="$alert_webhook_file" \
    --dry-run=client -o yaml | kube apply -f - >/dev/null

info "runtime Secret 4개를 생성 또는 교체했습니다. 값은 출력하지 않았습니다."
info "기존 MySQL PVC의 DB 자격증명 변경은 거부됩니다. 일반 app Secret 변경은 app 재기동 후 반영됩니다."
info "데이터 결합 키·키링과 Redis 비밀번호 변경은 일반 Secret 교체에서 거부됩니다."
