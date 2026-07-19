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
    TOSS_SECRET_KEY GOOGLE_OAUTH_CLIENT_ID GOOGLE_OAUTH_CLIENT_SECRET \
    NAVER_OAUTH_CLIENT_ID NAVER_OAUTH_CLIENT_SECRET KAKAO_API_KEY KAKAO_SENDER_KEY \
    SMS_API_KEY SMS_API_SECRET SMS_SENDER_NUMBER; do
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

[ "$mysql_database" = happygallery ] || die "MYSQL_DATABASE는 manifest의 happygallery와 같아야 합니다."
[ "$mysql_user" = "$db_user" ] || die "MYSQL_USER와 DB_USERNAME이 다릅니다."
[ "$mysql_password" = "$db_password" ] || die "MYSQL_PASSWORD와 DB_PASSWORD가 다릅니다."
[ "${#mysql_password}" -ge 24 ] || die "MySQL 애플리케이션 비밀번호는 24자 이상이어야 합니다."
[ "${#mysql_root_password}" -ge 24 ] || die "MySQL root 비밀번호는 24자 이상이어야 합니다."
[ "${#redis_password}" -ge 32 ] || die "Redis 비밀번호는 32자 이상이어야 합니다."
[ "${#guest_token_key}" -ge 32 ] || die "GUEST_TOKEN_HMAC_SECRET은 32자 이상이어야 합니다."
printf '%s' "$encrypt_key" | grep -Eq '^[A-Fa-f0-9]{64}$' || die "ENCRYPT_KEY는 64자리 hex여야 합니다."
printf '%s' "$hmac_key" | grep -Eq '^[A-Fa-f0-9]{64}$' || die "HMAC_KEY는 64자리 hex여야 합니다."

kube apply -f "$BASE_DIR/namespace.yaml" >/dev/null

if kube -n "$NAMESPACE" get secret happygallery-app >/dev/null 2>&1; then
    for key_and_value in \
        "ENCRYPT_KEY|$encrypt_key" \
        "HMAC_KEY|$hmac_key" \
        "GUEST_TOKEN_HMAC_SECRET|$guest_token_key"; do
        key=${key_and_value%%|*}
        desired_value=${key_and_value#*|}
        current_encoded=$(kube -n "$NAMESPACE" get secret happygallery-app \
            -o "jsonpath={.data.$key}")
        desired_encoded=$(base64_value "$desired_value")
        [ "$current_encoded" = "$desired_encoded" ] \
            || die "$key 변경은 기존 데이터와 토큰을 깨뜨립니다. 별도 키 회전 절차가 구현될 때까지 기존 값을 유지하세요."
    done
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
info "데이터 결합 키와 Redis 비밀번호 변경은 일반 Secret 교체에서 거부됩니다."
