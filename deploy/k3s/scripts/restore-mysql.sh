#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 3 ] || die "사용법: $0 <backup.sql.gz.age> <age identity 파일> <호환 release 디렉터리>"
backup=$1
identity=$2
release_dir=$3
[ -f "$backup" ] || die "백업 파일을 찾을 수 없습니다: $backup"
require_private_file "$identity"
require_command age
require_command gzip

[ "${CONFIRM_RESTORE:-}" = "restore-happygallery" ] \
    || die "CONFIRM_RESTORE=restore-happygallery 를 지정해야 합니다."

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -eq 0 ] \
    || die "복원 중 쓰기를 막기 위해 deployment/app을 먼저 0 replica로 축소하세요."
info "종료 중인 app Pod가 모두 사라질 때까지 기다립니다."
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120
remaining_app_pods=$(kube -n "$NAMESPACE" get pods -l app.kubernetes.io/name=app -o name) \
    || die "app Pod 목록을 최종 확인할 수 없습니다."
[ -z "$remaining_app_pods" ] \
    || die "app Pod가 남아 있어 복원을 중단합니다."

"$SCRIPT_DIR/prepare-restored-release-images.sh" "$release_dir"
verify_checksum "$backup"
info "age 인증과 gzip 스트림 무결성을 먼저 검사합니다."
age --decrypt -i "$identity" "$backup" | gzip -t

kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null
info "MySQL에 논리 백업을 복원합니다. 이 작업은 현재 DB 내용을 덮어씁니다."
kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
      "DROP DATABASE IF EXISTS \`happygallery\`; CREATE DATABASE \`happygallery\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
'
age --decrypt -i "$identity" "$backup" \
    | gzip -dc \
    | kube -n "$NAMESPACE" exec -i mysql-0 -- sh -ec \
        'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'

kube -n "$NAMESPACE" exec mysql-0 -- sh -ec \
    'exec mysqlcheck --check --all-databases -uroot -p"$MYSQL_ROOT_PASSWORD"' >/dev/null

info "DB 시점과 불일치하는 세션/처리율 상태를 Redis에서 제거합니다."
kube -n "$NAMESPACE" exec deployment/redis -- sh -ec \
    'REDISCLI_AUTH="$REDIS_PASSWORD" exec redis-cli FLUSHALL' >/dev/null

info "복원 완료. activate-restored-release.sh로 검증한 호환 digest를 지정한 뒤 app을 기동하고 전체 검증하세요."
