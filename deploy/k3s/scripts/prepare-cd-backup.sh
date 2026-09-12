#!/usr/bin/env bash
set -Eeuo pipefail
. "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[[ $# -eq 2 ]] || die "사용법: $0 <cd-backup.env> <CD 백업 검증 디렉터리>"
cd_backup_env=$1
cd_backup_root=$2
require_private_file "$cd_backup_env"
validate_env_file "$cd_backup_env"
RCLONE_BIN=$(require_env_value RCLONE_BIN "$cd_backup_env")
RCLONE_CONFIG=$(require_env_value RCLONE_CONFIG "$cd_backup_env")
RCLONE_BACKUP_REMOTE=$(require_env_value RCLONE_BACKUP_REMOTE "$cd_backup_env")
export RCLONE_BIN RCLONE_CONFIG RCLONE_BACKUP_REMOTE
bash "$SCRIPT_DIR/rclone-backup.sh" check >&2
umask 077
mkdir -p "$cd_backup_root"
cd_backup_listing=$(mktemp)
trap 'rm -f "$cd_backup_listing"' EXIT
"$RCLONE_BIN" --config "$RCLONE_CONFIG" --contimeout 10s --timeout 2m --retries 2 \
    lsf "$RCLONE_BACKUP_REMOTE" --max-depth 1 --files-only > "$cd_backup_listing"
cd_backup_name=$(ruby -rtime - "$cd_backup_listing" <<'RUBY'
names = File.readlines(ARGV.fetch(0), chomp: true).grep(/\Ahappygallery-\d{8}T\d{6}Z\.recovery\.env\z/)
latest = names.max or abort 'R2에 완성된 복구 묶음이 없습니다.'
created = Time.strptime(latest.delete_prefix('happygallery-').delete_suffix('.recovery.env'), '%Y%m%dT%H%M%S%Z').utc
age = Time.now.utc - created
abort 'R2 백업이 48시간보다 오래됐거나 미래 시각입니다.' unless age.between?(-300, 48 * 60 * 60)
puts latest
RUBY
)
cd_backup_stamp=${cd_backup_name#happygallery-}
cd_backup_stamp=${cd_backup_stamp%.recovery.env}
cd_backup_destination="$cd_backup_root/$cd_backup_stamp"
if [[ ! -d $cd_backup_destination ]]; then
    bash "$SCRIPT_DIR/rclone-backup.sh" download "$cd_backup_name" "$cd_backup_destination" >&2
fi
verify_recovery_bundle_files "$cd_backup_destination/$cd_backup_name" >&2
[[ $(require_env_value BACKUP_CREATED_AT "$cd_backup_destination/$cd_backup_name") == "$cd_backup_stamp" ]] \
    || die "백업의 생성 시각과 파일명이 다릅니다."

# 이 스크립트가 만든 검증 캐시만 두 세대 보관한다. 원격 백업과 운영 PVC는 건드리지 않는다.
ruby -rfileutils - "$cd_backup_root" "$cd_backup_stamp" <<'RUBY'
root, current = ARGV
names = Dir.children(root).grep(/\A\d{8}T\d{6}Z\z/).sort.reverse
keep = [current, (names - [current]).first].compact
(names - keep).each do |name|
  path = File.join(root, name)
  FileUtils.remove_entry_secure(path) if File.directory?(path) && !File.symlink?(path)
end
RUBY
printf '%s\n' "$cd_backup_destination/$cd_backup_name"
