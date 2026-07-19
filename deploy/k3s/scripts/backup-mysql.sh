#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

: "${BACKUP_DIR:?BACKUP_DIR가 필요합니다.}"
: "${BACKUP_AGE_RECIPIENT:?BACKUP_AGE_RECIPIENT가 필요합니다.}"

require_command age
require_command gzip
marker=${BACKUP_TARGET_MARKER:-$BACKUP_DIR/.happygallery-off-device-backup-target}
[ -f "$marker" ] \
    || die "외부 백업 매체 marker가 없습니다. 매체가 실제로 mount됐는지 확인하세요: $marker"

kube -n "$NAMESPACE" get pod mysql-0 >/dev/null
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null

timestamp=$(date -u '+%Y%m%dT%H%M%SZ')
backup="$BACKUP_DIR/happygallery-$timestamp.sql.gz.age"
tmp="$backup.partial"
umask 077
rm -f "$tmp"
trap 'rm -f "$tmp"' EXIT HUP INT TERM

info "MySQL 논리 무결성 검사를 실행합니다."
kube -n "$NAMESPACE" exec mysql-0 -- sh -ec \
    'exec mysqlcheck --check --all-databases -uroot -p"$MYSQL_ROOT_PASSWORD"' >/dev/null

info "MySQL 백업을 외부 매체에 age 암호화합니다."
kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysqldump \
      --single-transaction \
      --routines \
      --events \
      --triggers \
      --hex-blob \
      --no-tablespaces \
      --set-gtid-purged=OFF \
      --databases "$MYSQL_DATABASE" \
      -uroot -p"$MYSQL_ROOT_PASSWORD"
' | gzip -9 | age -r "$BACKUP_AGE_RECIPIENT" -o "$tmp"

[ -s "$tmp" ] || die "생성된 백업 파일이 비어 있습니다."
mv "$tmp" "$backup"
trap - EXIT HUP INT TERM
checksum=$(sha256_file "$backup")
printf '%s  %s\n' "$checksum" "$(basename -- "$backup")" > "$backup.sha256"
chmod 600 "$backup" "$backup.sha256"

info "암호화 백업 완료: $backup"
printf '%s\n' "$backup"
