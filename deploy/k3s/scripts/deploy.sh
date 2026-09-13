#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

imported=false
if [ "${1:-}" = --imported ]; then
    imported=true
    shift
fi
[ "$#" -ge 1 ] && [ "$#" -le 2 ] || die "사용법: $0 [--imported] <release.env> [git-ref]"
release_env=$(ruby -e 'puts File.realpath(ARGV.fetch(0))' "$1")
requested_ref=${2:-HEAD}
systemctl_bin=${SYSTEMCTL_BIN:-systemctl}
require_command flock
require_command "$systemctl_bin"
ruby "$SCRIPT_DIR/update-release-images.rb" --check "$release_env"

cd "$REPO_ROOT"
image_tag=$(git rev-parse --verify "$requested_ref^{commit}")
state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
umask 077
mkdir -p "$state_root"
exec 8> "$state_root/.deploy.lock"
flock -n 8 || die "다른 자동 배포가 진행 중입니다."

if [ "$imported" = false ]; then
    "$SCRIPT_DIR/build-import-images.sh" "$image_tag"
fi

app_image="localhost/happygallery-app:$image_tag"
frontend_image="localhost/happygallery-frontend:$image_tag"
app_digest=$(containerd_image_digest "$app_image") || die "반입된 앱 이미지를 찾을 수 없습니다."
frontend_digest=$(containerd_image_digest "$frontend_image") || die "반입된 프런트 이미지를 찾을 수 없습니다."
for pair in "$app_image|$app_digest" "$frontend_image|$frontend_digest"; do
    image=${pair%%|*}
    digest=${pair#*|}
    printf '%s' "$digest" | grep -Eq '^sha256:[a-f0-9]{64}$' || die "이미지 digest 형식이 올바르지 않습니다."
    for reference in "$image@$digest" "${image%:*}@$digest"; do
        actual=$(containerd_image_digest "$reference") || die "이미지의 digest 별칭이 없습니다: $reference"
        [ "$actual" = "$digest" ] || die "이미지 별칭의 digest가 다릅니다: $reference"
    done
done

systemctl_write() {
    if [ "$(id -u)" -eq 0 ]; then
        "$systemctl_bin" "$@"
    else
        sudo -- "$systemctl_bin" "$@"
    fi
}

trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

# 배포마다 현재 실행 중인 release를 먼저 백업한다. 실행 중인 백업이 있으면
# 끝날 때까지 기다린 뒤 새 백업을 시작해 백업과 rollout이 겹치지 않게 한다.
for ((attempt = 0; ; attempt++)); do
    backup_state=$("$systemctl_bin" show happygallery-backup.service --property=ActiveState --value)
    case "$backup_state" in
        inactive|failed|'') break ;;
        active|activating|deactivating|reloading)
            [ "$attempt" -lt 360 ] || die "백업 종료 대기가 30분을 넘었습니다. 백업 상태를 확인하세요."
            if ((attempt % 6 == 0)); then info "실행 중인 백업이 끝나기를 기다립니다."; fi
            sleep 5
            ;;
        *) die "알 수 없는 백업 서비스 상태입니다: $backup_state" ;;
    esac
done

systemctl_write start --wait happygallery-backup.service
cd_backup_env=${CD_BACKUP_ENV:-/etc/happygallery/cd-backup.env}
cd_backup_root=${HAPPYGALLERY_CD_BACKUP_DIR:-${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}/../cd/backups}
[ -f "$cd_backup_env" ] || die "배포용 백업 검증 설정이 없습니다: $cd_backup_env"
recovery_bundle=$(bash "$SCRIPT_DIR/prepare-cd-backup.sh" "$cd_backup_env" "$cd_backup_root")
export VERIFIED_RECOVERY_BUNDLE_OVERRIDE="$recovery_bundle"
info "배포 전 백업과 R2 복구 묶음 검증 완료: $recovery_bundle"

ruby "$SCRIPT_DIR/update-release-images.rb" "$release_env" "$image_tag" "$app_digest" "$frontend_digest"
"$SCRIPT_DIR/rollout.sh" "$release_env"
info "자동 배포 완료: $image_tag"
