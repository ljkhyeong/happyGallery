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
    'DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS=500' \
    'PUBLIC_HOLIDAY_ENABLED=false' \
    'PUBLIC_HOLIDAY_TIMEOUT_MILLIS=5000' \
    'PUBLIC_HOLIDAY_CONNECT_TIMEOUT_MILLIS=1000' \
    'PUBLIC_HOLIDAY_ACQUIRE_TIMEOUT_MILLIS=500' > "$app_file"
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
        | grep -Eq '허용되지 않은 키.*(PAYMENT_TIMEOUT_MILLIS|TOSS_TIMEOUT_MILLIS|TOSS_CONNECT_TIMEOUT_MILLIS|TOSS_ACQUIRE_TIMEOUT_MILLIS|DELIVERY_TRACKING_ENABLED|DELIVERY_API_TIMEOUT_MILLIS|DELIVERY_API_CONNECT_TIMEOUT_MILLIS|DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS|PUBLIC_HOLIDAY_ENABLED|PUBLIC_HOLIDAY_TIMEOUT_MILLIS|PUBLIC_HOLIDAY_CONNECT_TIMEOUT_MILLIS|PUBLIC_HOLIDAY_ACQUIRE_TIMEOUT_MILLIS)'; then
    printf '%s\n' "$output" >&2
    printf '결제·배송조회·공휴일 운영 설정이 허용 목록에서 거부되었습니다.\n' >&2
    exit 1
fi
printf '%s' "$output" | grep -q 'MYSQL_ROOT_PASSWORD' || {
    printf '%s\n' "$output" >&2
    printf '허용 목록 다음 단계까지 진행하지 못했습니다.\n' >&2
    exit 1
}

# 아래 자격 증명은 로컬 검사 전용이다. kubectl 대역으로 실제 클러스터에는 접근하지 않는다.
cat > "$mysql_file" <<'EOF'
MYSQL_DATABASE=happygallery
MYSQL_USER=gallery
MYSQL_PASSWORD=local-test-mysql-password-1234
MYSQL_ROOT_PASSWORD=local-test-root-password-12345
EOF
printf 'REDIS_PASSWORD=local-test-redis-password-1234567890\n' > "$redis_file"
cat > "$app_file" <<'EOF'
DB_USERNAME=gallery
DB_PASSWORD=local-test-mysql-password-1234
FIELD_ENCRYPTION_KEY_ID=v1
ENCRYPT_KEY=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
HMAC_KEY=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
GUEST_TOKEN_HMAC_SECRET=local-test-guest-token-12345678901234
TOSS_SECRET_KEY=test
GOOGLE_OAUTH_CLIENT_ID=test
GOOGLE_OAUTH_CLIENT_SECRET=test
NAVER_OAUTH_CLIENT_ID=test
NAVER_OAUTH_CLIENT_SECRET=test
KAKAO_OAUTH_CLIENT_ID=test
KAKAO_OAUTH_CLIENT_SECRET=test
ALIMTALK_APP_KEY=test
ALIMTALK_SECRET_KEY=test
ALIMTALK_SENDER_KEY=test
SMS_API_KEY=test
SMS_API_SECRET=test
SMS_SENDER_NUMBER=test
EMAIL_VERIFICATION_PROVIDER=ncp
NCP_MAIL_ACCESS_KEY=test-access-key
NCP_MAIL_SECRET_KEY=test-secret-key
NCP_MAIL_TIMEOUT_MILLIS=2000
NCP_MAIL_CONNECT_TIMEOUT_MILLIS=1000
NCP_MAIL_ACQUIRE_TIMEOUT_MILLIS=500
EMAIL_VERIFICATION_FROM=no-reply@mail.happy-gallery.com
EOF
cat > "$state_dir/kubectl" <<'EOF'
#!/usr/bin/env sh
printf 'called\n' >> "$HG_TEST_KUBE_LOG"
case "$*" in
    *get*) exit 1 ;;
    'apply -f -') cat >/dev/null ;;
esac
EOF
chmod 700 "$state_dir/kubectl"
export HG_TEST_KUBE_LOG="$state_dir/kube.log"

run_secret_test() {
    KUBECTL_BIN="$state_dir/kubectl" bash "$script_dir/create-secrets.sh" \
        "$mysql_file" "$redis_file" "$app_file" "$webhook_file"
}

run_secret_test >/dev/null
[ -s "$HG_TEST_KUBE_LOG" ] || { printf '네이버 키만으로 Secret 생성에 도달하지 못했습니다.\n' >&2; exit 1; }
cp "$app_file" "$state_dir/ncp.env"

expect_rejection() {
    local expected=$1
    : > "$HG_TEST_KUBE_LOG"
    if output=$(run_secret_test 2>&1); then
        printf '잘못된 메일 설정이 허용되었습니다: %s\n' "$expected" >&2
        exit 1
    fi
    printf '%s' "$output" | grep -q "$expected"
    [ ! -s "$HG_TEST_KUBE_LOG" ] || { printf '메일 설정 거부 전에 클러스터를 변경했습니다.\n' >&2; exit 1; }
}

sed '/^NCP_MAIL_SECRET_KEY=/d' "$state_dir/ncp.env" > "$app_file"
expect_rejection NCP_MAIL_SECRET_KEY
sed 's/^EMAIL_VERIFICATION_PROVIDER=ncp/EMAIL_VERIFICATION_PROVIDER=typo/' "$state_dir/ncp.env" > "$app_file"
expect_rejection EMAIL_VERIFICATION_PROVIDER
sed 's/^EMAIL_VERIFICATION_PROVIDER=ncp/EMAIL_VERIFICATION_PROVIDER=/' "$state_dir/ncp.env" > "$app_file"
expect_rejection EMAIL_VERIFICATION_PROVIDER

# provider가 없던 기존 파일은 SMTP로 동작하며 네이버 키를 요구하지 않는다.
sed '/^EMAIL_VERIFICATION_PROVIDER=/d; /^NCP_MAIL_/d' "$state_dir/ncp.env" > "$app_file"
expect_rejection EMAIL_VERIFICATION_SMTP_HOST
cat >> "$app_file" <<'EOF'
EMAIL_VERIFICATION_SMTP_HOST=smtp.example.com
EMAIL_VERIFICATION_SMTP_USERNAME=test
EMAIL_VERIFICATION_SMTP_PASSWORD=test
EOF
run_secret_test >/dev/null
printf 'runtime Secret 환경 키와 메일 제공자별 검증 완료\n'
