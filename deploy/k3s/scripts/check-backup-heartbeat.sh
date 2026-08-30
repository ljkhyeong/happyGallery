#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 2 ] || die "사용법: $0 <heartbeat 파일> <최대 경과 초>"
heartbeat_file=$1
max_age_seconds=$2

printf '%s' "$max_age_seconds" | grep -Eq '^[1-9][0-9]*$' \
    || die "최대 경과 초는 양의 정수여야 합니다."
[ -f "$heartbeat_file" ] || die "백업 성공 heartbeat가 없습니다: $heartbeat_file"

if modified_epoch=$(stat -c '%Y' "$heartbeat_file" 2>/dev/null); then
    :
elif modified_epoch=$(stat -f '%m' "$heartbeat_file" 2>/dev/null); then
    :
else
    die "백업 성공 heartbeat 수정 시각을 확인할 수 없습니다: $heartbeat_file"
fi

now_epoch=$(date '+%s')
age_seconds=$((now_epoch - modified_epoch))
[ "$age_seconds" -ge 0 ] || age_seconds=0
[ "$age_seconds" -le "$max_age_seconds" ] \
    || die "백업 성공 heartbeat가 정체됐습니다: age=${age_seconds}s max=${max_age_seconds}s"

info "백업 성공 heartbeat 정상: age=${age_seconds}s"
