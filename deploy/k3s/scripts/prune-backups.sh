#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

: "${BACKUP_DIR:?BACKUP_DIR가 필요합니다.}"
retention_days=${BACKUP_RETENTION_DAYS:-30}
marker=${BACKUP_TARGET_MARKER:-$BACKUP_DIR/.happygallery-off-device-backup-target}
[ -f "$marker" ] || die "외부 백업 매체 marker가 없어 보존 정리를 중단합니다: $marker"
printf '%s' "$retention_days" | grep -Eq '^[1-9][0-9]*$' || die "BACKUP_RETENTION_DAYS는 양의 정수여야 합니다."
[ "$retention_days" -ge 7 ] || die "백업 보존 기간은 최소 7일이어야 합니다."

find "$BACKUP_DIR" -maxdepth 1 -type f \
    \( -name 'happygallery-*.sql.gz.age' \
       -o -name 'happygallery-*.sql.gz.age.sha256' \
       -o -name 'happygallery-*.recovery.env' \
       -o -name 'happygallery-*.recovery.env.sha256' \) \
    -mtime "+$retention_days" -print -delete

info "$retention_days일보다 오래된 DB 백업과 복구 메타데이터를 정리했습니다. release archive는 수동 검증 후 보존합니다."
