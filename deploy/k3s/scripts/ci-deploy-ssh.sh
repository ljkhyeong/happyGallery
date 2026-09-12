#!/usr/bin/env bash
set -Eeuo pipefail

[[ ${GITHUB_SHA:-} =~ ^[a-f0-9]{40}$ ]] || { echo '잘못된 commit SHA' >&2; exit 1; }
[[ ${APP_REGISTRY_DIGEST:-} =~ ^sha256:[a-f0-9]{64}$ ]] || exit 1
[[ ${FRONTEND_REGISTRY_DIGEST:-} =~ ^sha256:[a-f0-9]{64}$ ]] || exit 1
[[ ${CD_HOST:-} =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]+$ ]] || { echo 'CD_HOST가 필요합니다.' >&2; exit 1; }
[[ ${CD_USER:-} =~ ^[a-z_][a-z0-9_-]*$ ]] || { echo 'CD_USER가 필요합니다.' >&2; exit 1; }
[[ ${CD_PORT:-} =~ ^[0-9]{1,5}$ ]] && ((10#$CD_PORT >= 1 && 10#$CD_PORT <= 65535)) || exit 1
: "${CD_SSH_PRIVATE_KEY:?CD_SSH_PRIVATE_KEY가 필요합니다.}"
: "${CD_SSH_KNOWN_HOSTS:?CD_SSH_KNOWN_HOSTS가 필요합니다.}"

umask 077
cd_credentials=$(mktemp -d)
trap 'rm -rf "$cd_credentials"' EXIT
printf '%s\n' "$CD_SSH_PRIVATE_KEY" > "$cd_credentials/key"
printf '%s\n' "$CD_SSH_KNOWN_HOSTS" > "$cd_credentials/known_hosts"
unset CD_SSH_PRIVATE_KEY CD_SSH_KNOWN_HOSTS

ssh -T -i "$cd_credentials/key" -p "$CD_PORT" \
    -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes \
    -o "UserKnownHostsFile=$cd_credentials/known_hosts" \
    -o ConnectTimeout=10 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 \
    "$CD_USER@$CD_HOST" \
    "deploy $GITHUB_SHA $APP_REGISTRY_DIGEST $FRONTEND_REGISTRY_DIGEST"
