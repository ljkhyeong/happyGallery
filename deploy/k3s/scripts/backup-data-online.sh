#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 4 ] || die "사용법: $0 <DB 암호문> <미디어 암호문> <app digest 이미지> <app replicas>"
: "${BACKUP_AGE_RECIPIENT:?BACKUP_AGE_RECIPIENT가 필요합니다.}"
db_output=$1
media_output=$2
app_image=$3
app_replicas=$4
[ ! -e "$db_output" ] && [ ! -e "$media_output" ] || die "백업 출력 파일이 이미 존재합니다."
case "$app_replicas" in
    1)
        kube -n "$NAMESPACE" exec deployment/app -- test -f /app/media-backup-guard-v1 \
            || die "실행 중인 앱이 온라인 백업을 지원하지 않습니다. 새 앱 이미지를 먼저 배포하세요."
        ;;
    0) ;; # 키 회전 등으로 이미 중지된 앱은 그대로 둔다.
    *) die "백업은 app 0 또는 1 replica 구성에서 실행해야 합니다." ;;
esac

non_transactional=$(mysql_database_query --execute="
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' AND ENGINE <> 'InnoDB';")
[ "$non_transactional" = 0 ] || die "온라인 스냅샷을 보장할 수 없는 비 InnoDB 테이블이 있습니다."

guard_owner="$(date -u '+%s')-$$"
MEDIA_HELPER_POD="media-backup-$guard_owner"
guard_owned=false
helper_started=false
completed=false

release_media_guard() {
    kube -n "$NAMESPACE" exec "$(media_helper_pod_name)" -- sh -ec '
        [ "$(cat /media/.backup-in-progress/owner)" = "$1" ]
        rm /media/.backup-in-progress/owner
        rmdir /media/.backup-in-progress
    ' sh "$guard_owner" || return 1
    guard_owned=false
}

cleanup_online_backup() {
    result=$?
    trap - EXIT HUP INT TERM
    if [ "$guard_owned" = true ]; then
        release_media_guard || result=1
    fi
    if [ "$helper_started" = true ]; then
        stop_media_helper
    fi
    if [ "$result" -ne 0 ] || [ "$completed" != true ]; then
        rm -f "$db_output" "$media_output"
        [ "$result" -ne 0 ] || result=1
    fi
    exit "$result"
}
trap cleanup_online_backup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

ensure_media_pvc
helper_started=true
start_media_helper "$app_image"
kube -n "$NAMESPACE" exec "$(media_helper_pod_name)" -- sh -ec '
    mkdir /media/.backup-in-progress
    printf "%s\n" "$1" > /media/.backup-in-progress/owner
' sh "$guard_owner"
guard_owned=true

# 이미 시작된 파일 삭제가 끝날 때까지만 기다린다. 일반 조회·주문에는 잠금을 걸지 않는다.
barrier=$(mysql_database_query --execute="
    SET SESSION innodb_lock_wait_timeout = 15;
    START TRANSACTION;
    SELECT id FROM image_media_reference_lock WHERE id = 1 FOR UPDATE;
    COMMIT;")
[ "$barrier" = 1 ] || die "이미지 삭제와 백업 사이의 잠금을 확인하지 못했습니다."

info "앱을 유지한 채 InnoDB 트랜잭션 스냅샷을 암호화합니다."
kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysqldump --single-transaction --quick \
      --routines --events --triggers --hex-blob --no-tablespaces \
      --set-gtid-purged=OFF --skip-lock-tables \
      --databases "$MYSQL_DATABASE" -uroot -p"$MYSQL_ROOT_PASSWORD"
' | gzip -9 | age -r "$BACKUP_AGE_RECIPIENT" -o "$db_output"
[ -s "$db_output" ] || die "DB 백업 암호문이 비어 있습니다."

# 완료된 이미지 이름만 고정한다. 업로드 임시 파일과 변경되는 디렉터리는 tar에 넣지 않는다.
info "물리적 삭제를 보류한 이미지 파일을 암호화합니다."
kube -n "$NAMESPACE" exec "$(media_helper_pod_name)" -- sh -ec '
    cd /media
    find . -maxdepth 1 -type f \( -name "*.jpg" -o -name "*.png" -o -name "*.webp" \) \
      > /tmp/media-backup-files
    if [ -s /tmp/media-backup-files ]; then
        tar -cf - -T /tmp/media-backup-files
    else
        # Alpine tar는 빈 목록 생성을 거부하므로 표준 종료 블록 두 개를 보낸다.
        dd if=/dev/zero bs=512 count=2 2>/dev/null
    fi
' | gzip -9 | age -r "$BACKUP_AGE_RECIPIENT" -o "$media_output"
[ -s "$media_output" ] || die "미디어 백업 암호문이 비어 있습니다."

release_media_guard
stop_media_helper
helper_started=false
completed=true
info "DB·미디어 온라인 백업 완료: app replica를 변경하지 않았습니다."
