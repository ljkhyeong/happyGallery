#!/usr/bin/env bash

set -Eeuo pipefail

state_dir=${FAKE_KUBE_STATE:?FAKE_KUBE_STATE is required}
new_root=${EXPECTED_NEW_ROOT:?EXPECTED_NEW_ROOT is required}
new_app=${EXPECTED_NEW_APP:?EXPECTED_NEW_APP is required}

if [ "${1:-}" = -n ]; then
    shift 2
fi

read_state() {
    if [ -f "$state_dir/$1" ]; then
        cat "$state_dir/$1"
    fi
}

write_state() {
    printf '%s' "$2" > "$state_dir/$1"
}

case "${1:-}" in
    get)
        resource=${2:-}
        name=${3:-}
        arguments=$*
        case "$resource:$name:$arguments" in
            secret:happygallery-mysql:*MYSQL_USER*)
                printf '%s' "$(read_state mysql_user)" | base64 | tr -d '\n'
                ;;
            secret:happygallery-mysql:*MYSQL_ROOT_PASSWORD*)
                printf '%s' "$(read_state mysql_secret_root)" | base64 | tr -d '\n'
                ;;
            secret:happygallery-mysql:*MYSQL_PASSWORD*)
                printf '%s' "$(read_state mysql_secret_app)" | base64 | tr -d '\n'
                ;;
            secret:happygallery-app:*DB_PASSWORD*)
                printf '%s' "$(read_state app_secret_password)" | base64 | tr -d '\n'
                ;;
            secret:happygallery-mysql:*mysql-rotation-phase*)
                read_state phase
                ;;
            secret:happygallery-mysql:*mysql-rotation-target*)
                read_state target
                ;;
            secret:happygallery-mysql:*mysql-rotation-original-replicas*)
                read_state original_replicas
                ;;
            deployment:app:*)
                read_state replicas
                ;;
        esac
        ;;
    patch)
        resource=${2:-}
        name=${3:-}
        body=$(cat)
        if [ "$resource:$name" = secret:happygallery-app ]; then
            if [ "${FAIL_ONCE_AT:-}" = app-secret ] && [ ! -f "$state_dir/app-secret-failed" ]; then
                touch "$state_dir/app-secret-failed"
                exit 70
            fi
            write_state app_secret_password "$new_app"
            exit 0
        fi
        if [ "$resource:$name" = secret:happygallery-mysql ]; then
            if printf '%s' "$body" | grep -q '"data"'; then
                write_state mysql_secret_root "$new_root"
                write_state mysql_secret_app "$new_app"
            fi
            phase=$(printf '%s' "$body" | sed -n 's/.*"happygallery.io\/mysql-rotation-phase":"\([^"]*\)".*/\1/p')
            target=$(printf '%s' "$body" | sed -n 's/.*"happygallery.io\/mysql-rotation-target":"\([^"]*\)".*/\1/p')
            replicas=$(printf '%s' "$body" | sed -n 's/.*"happygallery.io\/mysql-rotation-original-replicas":"\([^"]*\)".*/\1/p')
            [ -z "$phase" ] || write_state phase "$phase"
            [ -z "$target" ] || write_state target "$target"
            [ -z "$replicas" ] || write_state original_replicas "$replicas"
            if [ "$phase" = completed ] \
                && [ "${FAIL_ONCE_AT:-}" = completed-response ] \
                && [ ! -f "$state_dir/completed-response-failed" ]; then
                touch "$state_dir/completed-response-failed"
                exit 71
            fi
        fi
        ;;
    scale)
        replicas=${3#--replicas=}
        write_state replicas "$replicas"
        ;;
    exec)
        IFS= read -r username
        IFS= read -r password
        IFS= read -r sql
        if [ "$username" = root ]; then
            [ "$password" = "$(read_state db_root)" ] || exit 1
            if printf '%s' "$sql" | grep -q '^ALTER USER '; then
                if printf '%s' "$sql" | grep -q "'root'@'localhost'"; then
                    write_state db_root "$new_root"
                fi
                write_state db_app "$new_app"
                count=$(read_state alter_count)
                write_state alter_count "$((count + 1))"
            fi
        else
            [ "$username" = "$(read_state mysql_user)" ] || exit 1
            [ "$password" = "$(read_state db_app)" ] || exit 1
        fi
        ;;
    wait|rollout)
        ;;
    *)
        printf '지원하지 않는 fake kubectl 호출: %s\n' "$*" >&2
        exit 64
        ;;
esac
