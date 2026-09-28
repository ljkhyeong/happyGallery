#!/usr/bin/env bash
# 원격 내용 검증을 끝낸 upload 프로세스가 보내는 묶음만 배포 계정으로 받는다.
set -Eeuo pipefail
. "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)/common.sh"
[[ $# -eq 3 ]] || die "사용법: $0 <캐시 경로> <복구 파일명> <원격 경로>"
cache_root=$1
name=$2
remote=$3
[[ $name =~ ^happygallery-[0-9]{8}T[0-9]{6}Z\.recovery\.env$ ]] || die "잘못된 복구 파일명입니다."
umask 077
mkdir -p "$cache_root"
stamp=${name#happygallery-}
stamp=${stamp%.recovery.env}
destination="$cache_root/$stamp"
temporary=$(mktemp -d "$cache_root/.upload-verified.XXXXXX")
trap 'rm -rf "$temporary"' EXIT
tar -xf - -C "$temporary"
verify_recovery_bundle_files "$temporary/$name"
printf '%s\n' "$remote" > "$temporary/.verified-remote"
if [[ -e $destination || -L $destination ]]; then
    die "검증 캐시가 이미 존재합니다: $destination"
fi
mv "$temporary" "$destination"
info "원격 검증을 마친 로컬 복구 묶음을 배포 캐시에 저장했습니다: $destination"
