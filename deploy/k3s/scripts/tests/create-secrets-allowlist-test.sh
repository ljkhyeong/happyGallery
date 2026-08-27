#!/usr/bin/env bash

set -Eeuo pipefail

test_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
script_dir=$(CDPATH= cd -- "$test_dir/.." && pwd)
state_dir=$(mktemp -d "${TMPDIR:-/tmp}/happygallery-secret-allowlist-test.XXXXXX")
trap 'rm -rf "$state_dir"' EXIT HUP INT TERM

mysql_file="$state_dir/mysql.env"
redis_file="$state_dir/redis.env"
app_file="$state_dir/app.env"
webhook_file="$state_dir/alert-webhook-url"

printf 'MYSQL_DATABASE=happygallery\n' > "$mysql_file"
printf 'REDIS_PASSWORD=test\n' > "$redis_file"
printf 'SPRING_PROFILES_ACTIVE=local\n' > "$app_file"
printf 'https://alerts.invalid/hook\n' > "$webhook_file"
chmod 600 "$mysql_file" "$redis_file" "$app_file" "$webhook_file"

set +e
output=$(KUBECTL_BIN=/bin/false \
    bash "$script_dir/create-secrets.sh" \
        "$mysql_file" "$redis_file" "$app_file" "$webhook_file" 2>&1)
status=$?
set -e

[ "$status" -ne 0 ] || {
    printf '허용 목록 밖 운영 profile 키가 거부되지 않았습니다.\n' >&2
    exit 1
}
printf '%s' "$output" | grep -q '허용되지 않은 키.*SPRING_PROFILES_ACTIVE' || {
    printf '%s\n' "$output" >&2
    printf '허용 목록 거부 원인을 확인할 수 없습니다.\n' >&2
    exit 1
}

printf '%s\n' \
    'PAYMENT_TIMEOUT_MILLIS=5000' \
    'TOSS_TIMEOUT_MILLIS=3000' \
    'TOSS_CONNECT_TIMEOUT_MILLIS=1000' \
    'TOSS_ACQUIRE_TIMEOUT_MILLIS=500' \
    'DELIVERY_TRACKING_ENABLED=false' \
    'DELIVERY_API_TIMEOUT_MILLIS=3000' \
    'DELIVERY_API_CONNECT_TIMEOUT_MILLIS=1000' \
    'DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS=500' > "$app_file"
set +e
output=$(KUBECTL_BIN=/bin/false \
    bash "$script_dir/create-secrets.sh" \
        "$mysql_file" "$redis_file" "$app_file" "$webhook_file" 2>&1)
status=$?
set -e

[ "$status" -ne 0 ] || {
    printf '불완전한 테스트 환경 파일이 정상 처리되었습니다.\n' >&2
    exit 1
}
if printf '%s' "$output" \
        | grep -Eq '허용되지 않은 키.*(PAYMENT_TIMEOUT_MILLIS|TOSS_TIMEOUT_MILLIS|TOSS_CONNECT_TIMEOUT_MILLIS|TOSS_ACQUIRE_TIMEOUT_MILLIS|DELIVERY_TRACKING_ENABLED|DELIVERY_API_TIMEOUT_MILLIS|DELIVERY_API_CONNECT_TIMEOUT_MILLIS|DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS)'; then
    printf '%s\n' "$output" >&2
    printf '결제·배송조회 운영 설정이 허용 목록에서 거부되었습니다.\n' >&2
    exit 1
fi
printf '%s' "$output" | grep -q 'MYSQL_ROOT_PASSWORD' || {
    printf '%s\n' "$output" >&2
    printf '허용 목록 다음 단계까지 진행하지 못했습니다.\n' >&2
    exit 1
}

printf 'runtime Secret 환경 키 허용 목록 검증 완료\n'
