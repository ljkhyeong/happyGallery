#!/usr/bin/env bash
set -Eeuo pipefail

# root 소유의 고정 경로에 설치하고 배포 전용 authorized_keys command에서 호출한다.
command_pattern='^deploy ([a-f0-9]{40}) (sha256:[a-f0-9]{64}) (sha256:[a-f0-9]{64})$'
[[ ${SSH_ORIGINAL_COMMAND:-} =~ $command_pattern ]] || { echo '허용된 배포 명령이 아닙니다.' >&2; exit 1; }
cd_revision=${BASH_REMATCH[1]}
cd_app_digest=${BASH_REMATCH[2]}
cd_frontend_digest=${BASH_REMATCH[3]}

cd_repository=/opt/happygallery
cd_state="$HOME/.local/state/happygallery/cd"
export HAPPYGALLERY_RELEASE_DIR="$HOME/.local/state/happygallery/releases"
export KUBECTL_BIN=/usr/local/libexec/happygallery-kubectl
export GIT_TERMINAL_PROMPT=0
umask 077
mkdir -p "$cd_state"
exec 9> "$cd_state/.lock"
flock -n 9 || { echo '다른 CD가 실행 중입니다.' >&2; exit 1; }
[[ $(git -C "$cd_repository" remote get-url origin) == https://github.com/ljkhyeong/happyGallery.git ]] \
    || { echo 'origin 저장소가 다릅니다.' >&2; exit 1; }
git -C "$cd_repository" fetch --no-tags origin refs/heads/main:refs/remotes/happygallery-cd/main
[[ $(git -C "$cd_repository" rev-parse refs/remotes/happygallery-cd/main) == "$cd_revision" ]] \
    || { echo 'main 최신 commit이 아니어서 배포하지 않습니다.' >&2; exit 1; }

# 기존 수동 작업 checkout은 변경하지 않고 같은 Git 이력을 공유한다.
cd_checkout="$cd_state/source"
if [[ -d $cd_checkout ]]; then
    [[ -z $(git -C "$cd_checkout" status --porcelain) ]] || { echo 'CD checkout이 변경됐습니다.' >&2; exit 1; }
    git -C "$cd_checkout" checkout --detach "$cd_revision"
else
    git -C "$cd_repository" worktree add --detach "$cd_checkout" "$cd_revision"
fi
cd "$cd_checkout"
bash deploy/k3s/scripts/deploy-registry-images.sh /etc/happygallery/release.env \
    "$cd_revision" "$cd_app_digest" "$cd_frontend_digest"
