#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <실패한 systemd unit>"
unit=$1
host=$(hostname)
if [ -n "${BACKUP_ALERT_CONFIG:-}" ] && [ -n "${BACKUP_ALERT_EMAIL_CONFIG:-}" ]; then
    die "BACKUP_ALERT_CONFIG와 이전 BACKUP_ALERT_EMAIL_CONFIG 중 하나만 설정하세요."
fi
alert_config=${BACKUP_ALERT_CONFIG:-${BACKUP_ALERT_EMAIL_CONFIG:-}}
if [ -n "$alert_config" ]; then
    [ -z "${BACKUP_ALERT_WEBHOOK_URL:-}" ] || die "백업 알림 설정 파일과 webhook 중 하나만 설정하세요."
    require_command ruby
    exec ruby "$SCRIPT_DIR/alert-delivery.rb" send \
        "${BACKUP_ALERT_APP_ENV:-/etc/happygallery/app.env}" "$alert_config" \
        'happyGallery 백업 운영 경보' "unit=$unit host=$host"
fi
: "${BACKUP_ALERT_WEBHOOK_URL:?BACKUP_ALERT_WEBHOOK_URL이 필요합니다.}"
case "$BACKUP_ALERT_WEBHOOK_URL" in
    https://*) ;;
    *) die "백업 실패 webhook은 HTTPS만 허용합니다." ;;
esac
require_command curl

payload=$(printf '{"text":"happyGallery 백업 운영 경보: unit=%s host=%s"}' "$unit" "$host")
curl --fail --silent --show-error \
    --connect-timeout 3 --max-time 10 \
    -H 'Content-Type: application/json' \
    --data "$payload" \
    "$BACKUP_ALERT_WEBHOOK_URL" >/dev/null
