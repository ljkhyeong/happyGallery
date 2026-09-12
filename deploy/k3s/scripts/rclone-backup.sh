#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

action=${1:-}
RCLONE_BIN=${RCLONE_BIN:-/usr/local/bin/rclone}
: "${RCLONE_CONFIG:?RCLONE_CONFIG가 필요합니다.}"
: "${RCLONE_BACKUP_REMOTE:?RCLONE_BACKUP_REMOTE가 필요합니다.}"
require_command "$RCLONE_BIN"
require_command ruby
require_private_file "$RCLONE_CONFIG"
# 버킷 전체 대신 전용 prefix만 다룬다. 동적 backend와 상대 경로는 받지 않는다.
printf '%s' "$RCLONE_BACKUP_REMOTE" | grep -Eq '^[A-Za-z0-9_-]+:[A-Za-z0-9][A-Za-z0-9.-]*/[A-Za-z0-9_-]+$' \
    || die "RCLONE_BACKUP_REMOTE는 remote:bucket/전용-prefix 형식이어야 합니다."
umask 077
work=$(mktemp -d "${TMPDIR:-/tmp}/happygallery-rclone.XXXXXX")
trap 'rm -rf "$work"' EXIT

rclone_run() {
    "$RCLONE_BIN" --config "$RCLONE_CONFIG" \
        --contimeout 10s --timeout 2m --retries 2 --low-level-retries 2 \
        --transfers 2 --checkers 2 --stats 30s --stats-one-line --log-level INFO "$@"
}

# 사용자가 지정한 복구 metadata에서 전송 가능한 정확한 파일명만 만든다.
bundle_files() {
    local metadata=$1 name tag prefix
    name=$(basename -- "$metadata")
    printf '%s' "$name" | grep -Eq '^happygallery-[0-9]{8}T[0-9]{6}Z\.recovery\.env$' \
        || die "복구 metadata 파일명이 올바르지 않습니다."
    require_private_file "$metadata"
    verify_checksum "$metadata"
    validate_env_file "$metadata"
    prefix=${name%.recovery.env}
    tag=$(require_env_value IMAGE_TAG "$metadata")
    printf '%s' "$tag" | grep -Eq '^[a-fA-F0-9]{12,40}$' || die "IMAGE_TAG 형식이 올바르지 않습니다."
    [ "$(require_env_value BACKUP_CREATED_AT "$metadata")" = "${prefix#happygallery-}" ] \
        || die "백업 시각과 파일명이 다릅니다."
    [ "$(require_env_value DATABASE_BACKUP "$metadata")" = "$prefix.sql.gz.age" ] \
        && [ "$(require_env_value MEDIA_BACKUP "$metadata")" = "$prefix.media.tar.gz.age" ] \
        && [ "$(require_env_value RELEASE_DIR "$metadata")" = "releases/$tag" ] \
        || die "복구 metadata의 파일 참조가 올바르지 않습니다."
    printf '%s\n' "$prefix.sql.gz.age" "$prefix.sql.gz.age.sha256" \
        "$prefix.media.tar.gz.age" "$prefix.media.tar.gz.age.sha256" "$name.sha256"
    for name in metadata.env manifests.yaml runtime-images.env images.tar; do
        printf 'releases/%s/%s\nreleases/%s/%s.sha256\n' "$tag" "$name" "$tag" "$name"
    done
}

case "$action" in
    check)
        [ "$#" -eq 1 ] || die "사용법: $0 check"
        rclone_run lsf "$RCLONE_BACKUP_REMOTE" --max-depth 1 --files-only > "$work/remote-files"
        info "R2 백업 경로 조회 OK"
        ;;
    upload)
        [ "$#" -eq 2 ] || die "사용법: $0 upload <recovery.env>"
        verify_recovery_bundle_files "$2"
        bundle_files "$2" > "$work/payload-files"
        [ ! -L "$bundle_root/releases" ] && [ ! -L "$bundle_release" ] \
            || die "release 백업 경로에 심볼릭 링크를 사용할 수 없습니다."
        while IFS= read -r relative; do
            [ ! -L "$bundle_root/$relative" ] || die "백업 파일에 심볼릭 링크를 사용할 수 없습니다."
        done < "$work/payload-files"
        info "암호화 DB·미디어와 호환 release를 R2에 전송합니다."
        rclone_run copy "$bundle_root" "$RCLONE_BACKUP_REMOTE" \
            --files-from-raw "$work/payload-files" --checksum --immutable
        # multipart ETag를 파일 해시로 간주하지 않고 실제 내용을 읽어 비교한다.
        rclone_run check "$bundle_root" "$RCLONE_BACKUP_REMOTE" \
            --files-from-raw "$work/payload-files" --download --one-way
        name=$(basename -- "$2")
        rclone_run copyto "$2" "$RCLONE_BACKUP_REMOTE/$name" --checksum --immutable
        printf '%s\n' "$name" > "$work/metadata-file"
        rclone_run check "$bundle_root" "$RCLONE_BACKUP_REMOTE" \
            --files-from-raw "$work/metadata-file" --download --one-way
        info "R2 백업 업로드·내용 검증 완료: $name"
        ;;
    download)
        [ "$#" -eq 3 ] || die "사용법: $0 download <recovery.env 파일명> <새 로컬 디렉터리>"
        name=$2
        destination=$3
        printf '%s' "$name" | grep -Eq '^happygallery-[0-9]{8}T[0-9]{6}Z\.recovery\.env$' \
            || die "복구 metadata 파일명이 올바르지 않습니다."
        [ ! -e "$destination" ] && [ ! -L "$destination" ] \
            || die "다운로드 대상은 아직 존재하지 않는 디렉터리여야 합니다."
        # 큰 이미지 archive를 /tmp에 중복 저장하지 않고 최종 대상과 같은 파일시스템에 받는다.
        download_dir=$(mktemp -d "${destination}.partial.XXXXXX")
        trap 'rm -rf "$work" "$download_dir"' EXIT
        rclone_run copyto "$RCLONE_BACKUP_REMOTE/$name" "$download_dir/$name" --ignore-times
        rclone_run copyto "$RCLONE_BACKUP_REMOTE/$name.sha256" "$download_dir/$name.sha256" --ignore-times
        chmod 600 "$download_dir/$name" "$download_dir/$name.sha256"
        bundle_files "$download_dir/$name" > "$work/payload-files"
        rclone_run copy "$RCLONE_BACKUP_REMOTE" "$download_dir" \
            --files-from-raw "$work/payload-files" --ignore-times
        find "$download_dir" -type d -exec chmod 700 {} +
        find "$download_dir" -type f -exec chmod 600 {} +
        verify_recovery_bundle_files "$download_dir/$name"
        [ ! -e "$destination" ] && [ ! -L "$destination" ] || die "다운로드 대상이 실행 중 생성됐습니다."
        mv "$download_dir" "$destination"
        info "R2 복구 묶음 다운로드·SHA-256 검증 완료: $destination/$name"
        ;;
    prune)
        [ "$#" -eq 1 ] || die "사용법: $0 prune"
        : "${BACKUP_DIR:?BACKUP_DIR가 필요합니다.}"
        retention_days=${BACKUP_RETENTION_DAYS:-30}
        require_command flock
        exec 9> "$BACKUP_DIR/.backup.lock"
        flock -n 9 || die "다른 백업 또는 보존 정리가 실행 중입니다."
        rclone_run lsf "$RCLONE_BACKUP_REMOTE" --max-depth 1 --files-only > "$work/remote-files"
        ruby -rtime - "$retention_days" "$work" <<'RUBY'
days, work = ARGV
abort '보존 기간은 최소 7일인 정수여야 합니다.' unless days.match?(/\A[0-9]+\z/) && days.to_i >= 7
cutoff = Time.now.utc - days.to_i * 86_400
files = File.readlines(File.join(work, 'remote-files'), chomp: true)
markers = files.grep(/\Ahappygallery-\d{8}T\d{6}Z\.recovery\.env\z/)
# 새 성공 백업이 없는데 보존 정리만 실행해서 마지막 복구본을 지우지 않는다.
latest = markers.max
abort '최근 완료된 R2 복구 묶음이 없어 보존 정리를 중단합니다.' unless
  latest && Time.strptime(latest.delete_prefix('happygallery-').delete_suffix('.recovery.env'), '%Y%m%dT%H%M%S%Z') >= cutoff
expired = files.select do |name|
  match = /\Ahappygallery-(\d{8}T\d{6}Z)\.(?:sql\.gz\.age|media\.tar\.gz\.age|recovery\.env)(?:\.sha256)?\z/.match(name)
  match && Time.strptime(match[1], '%Y%m%dT%H%M%S%Z') < cutoff
end
File.write(File.join(work, 'expired-markers'), expired.grep(/\.recovery\.env\z/).join("\n"))
File.write(File.join(work, 'expired-payloads'), expired.reject { |name| name.end_with?('.recovery.env') }.join("\n"))
RUBY
        # 완료 표시를 먼저 지워 정리 중인 묶음이 복원 후보에 남지 않게 한다.
        for list in expired-markers expired-payloads; do
            if [ -s "$work/$list" ]; then
                rclone_run delete "$RCLONE_BACKUP_REMOTE" --max-depth 1 --files-from-raw "$work/$list"
                while IFS= read -r relative || [ -n "$relative" ]; do
                    rm -f "$BACKUP_DIR/$relative"
                done < "$work/$list"
            fi
        done
        info "R2의 ${retention_days}일 초과 DB·미디어 백업을 정리했습니다. 공유 release archive는 보존합니다."
        ;;
    *) die "사용법: $0 <check|upload|download|prune> [인자...]" ;;
esac
