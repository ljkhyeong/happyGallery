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

live_container_image() {
    kube -n "$NAMESPACE" get deployment "$1" \
        -o "jsonpath={.spec.template.spec.containers[?(@.name==\"$1\")].image}"
}

release_runs_images() {
    local metadata=$1/metadata.env image digest
    [ -f "$metadata" ] || return 1
    image=$(env_value APP_IMAGE "$metadata") && digest=$(env_value APP_IMAGE_DIGEST "$metadata") || return 1
    case "$2" in "$image@$digest"|"${image%:*}@$digest") ;; *) return 1 ;; esac
    image=$(env_value FRONTEND_IMAGE "$metadata") && digest=$(env_value FRONTEND_IMAGE_DIGEST "$metadata") || return 1
    case "$3" in "$image@$digest"|"${image%:*}@$digest") ;; *) return 1 ;; esac
}

# 롤아웃 뒤 공개 점검이 실패한 이전 배포는 새 release를 실행하면서 current를 갱신하지 못했다.
# 백업은 current와 실행 이미지가 같아야 하므로, 실행 중인 이미지와 같은 release로 current를 맞춘다.
align_current_release() {
    local live_app live_frontend release
    live_app=$(live_container_image app 2>/dev/null) || return 0
    live_frontend=$(live_container_image frontend 2>/dev/null) || return 0
    [ -n "$live_app" ] && [ -n "$live_frontend" ] || return 0
    release_runs_images "$state_root/current" "$live_app" "$live_frontend" && return 0
    while IFS= read -r release; do
        if release_runs_images "$release" "$live_app" "$live_frontend"; then
            ln -sfn "$release" "$state_root/current"
            info "실행 중인 이미지에 맞춰 current release 기록을 바로잡았습니다: $release"
            return 0
        fi
    done < <(find "$state_root" -mindepth 1 -maxdepth 1 -type d -name '20*' | sort -r)
}

# sudoers 예시(cd-sudoers.example)와 인수가 정확히 같아야 비대화형으로 읽을 수 있다.
backup_journal() {
    if [ "$(id -u)" -eq 0 ]; then
        journalctl -u happygallery-backup.service --since -40min --no-pager -o cat
    else
        sudo -n -- journalctl -u happygallery-backup.service --since -40min --no-pager -o cat
    fi
}

# 배포 로그는 공개 저장소의 Actions에 남는다. 백업 스크립트의 오류·진행 문구만 옮기고 외부 도구 출력은 서버에서 본다.
report_backup_failure() {
    local journal
    "$systemctl_bin" show happygallery-backup.service \
        --property=Result --property=ExecMainStatus --property=ExecMainExitTimestamp 2>/dev/null \
        | sed 's/^/[백업 상태] /' >&2 || true
    if journal=$(backup_journal 2>/dev/null); then
        printf '%s\n' "$journal" | grep -E '^(오류: |\[happygallery\] )' | tail -n 20 \
            | sed 's/^/[백업 기록] /' >&2 || true
    else
        printf '%s\n' '[백업 기록] journal을 읽을 권한이 없습니다. 서버에서 journalctl -u happygallery-backup.service로 확인하세요.' >&2
    fi
}

online_backup_bootstrap=${HAPPYGALLERY_ONLINE_BACKUP_BOOTSTRAP:-false}
case "$online_backup_bootstrap" in
    true|false) ;;
    *) die "HAPPYGALLERY_ONLINE_BACKUP_BOOTSTRAP는 true 또는 false여야 합니다." ;;
esac

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

if [ "$online_backup_bootstrap" = true ]; then
    recovery_bundle=${VERIFIED_RECOVERY_BUNDLE_OVERRIDE:-}
    [ -n "$recovery_bundle" ] || die "온라인 백업 최초 전환에는 검증된 R2 복구 묶음이 필요합니다."
    verify_recovery_bundle_files "$recovery_bundle"
    [ -n "$(find "$recovery_bundle" -prune -mtime -2 -print)" ] \
        || die "온라인 백업 최초 전환에 사용할 R2 복구 묶음이 48시간보다 오래됐습니다."
    info "기존 앱이 온라인 백업을 지원하지 않아 검증된 R2 복구 묶음으로 최초 전환합니다: $recovery_bundle"
else
    align_current_release
    if ! systemctl_write start --wait happygallery-backup.service; then
        report_backup_failure
        die "배포 전 백업이 실패해 rollout을 시작하지 않습니다. 위 백업 상태·기록을 확인하세요."
    fi
    cd_backup_env=${CD_BACKUP_ENV:-/etc/happygallery/cd-backup.env}
    cd_backup_root=${HAPPYGALLERY_CD_BACKUP_DIR:-${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}/../cd/backups}
    [ -f "$cd_backup_env" ] || die "배포용 백업 검증 설정이 없습니다: $cd_backup_env"
    recovery_bundle=$(bash "$SCRIPT_DIR/prepare-cd-backup.sh" "$cd_backup_env" "$cd_backup_root")
    export VERIFIED_RECOVERY_BUNDLE_OVERRIDE="$recovery_bundle"
    info "배포 전 백업과 R2 복구 묶음 검증 완료: $recovery_bundle"
fi

ruby "$SCRIPT_DIR/update-release-images.rb" "$release_env" "$image_tag" "$app_digest" "$frontend_digest"
"$SCRIPT_DIR/rollout.sh" "$release_env"
info "자동 배포 완료: $image_tag"
