#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <실패한 systemd unit>"
: "${BACKUP_ALERT_WEBHOOK_URL:?BACKUP_ALERT_WEBHOOK_URL이 필요합니다.}"
case "$BACKUP_ALERT_WEBHOOK_URL" in
    https://*) ;;
    *) die "백업 실패 webhook은 HTTPS만 허용합니다." ;;
esac
require_command curl

unit=$1
host=$(hostname)
payload=$(printf '{"text":"happyGallery 백업 실패: unit=%s host=%s"}' "$unit" "$host")
curl --fail --silent --show-error \
    --connect-timeout 3 --max-time 10 \
    -H 'Content-Type: application/json' \
    --data "$payload" \
    "$BACKUP_ALERT_WEBHOOK_URL" >/dev/null
