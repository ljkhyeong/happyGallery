#!/usr/bin/env bash

set -Eeuo pipefail

test_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
script_dir=$(CDPATH= cd -- "$test_dir/.." && pwd)
state_dir=$(mktemp -d "${TMPDIR:-/tmp}/happygallery-mysql-rotation-test.XXXXXX")
trap 'rm -rf "$state_dir"' EXIT HUP INT TERM

old_root=old-root-password-0000000000000001
old_app=old-app-password-00000000000000001
new_root=new-root-password-0000000000000001
new_app=new-app-password-00000000000000001
rotation_file=$state_dir/mysql-rotation.env

printf '%s' happygallery > "$state_dir/mysql_user"
printf '%s' "$old_root" > "$state_dir/db_root"
printf '%s' "$old_app" > "$state_dir/db_app"
printf '%s' "$old_root" > "$state_dir/mysql_secret_root"
printf '%s' "$old_app" > "$state_dir/mysql_secret_app"
printf '%s' "$old_app" > "$state_dir/app_secret_password"
printf '%s' 1 > "$state_dir/replicas"
printf '%s' 0 > "$state_dir/alter_count"
printf 'MYSQL_ROOT_PASSWORD=%s\nMYSQL_PASSWORD=%s\n' "$new_root" "$new_app" > "$rotation_file"
chmod 600 "$rotation_file"

run_rotation() {
    CONFIRM_MYSQL_CREDENTIAL_ROTATION=rotate-happygallery-mysql \
    KUBECTL_BIN="$test_dir/fake-kubectl-for-mysql-rotation.sh" \
    FAKE_KUBE_STATE="$state_dir" \
    EXPECTED_NEW_ROOT="$new_root" \
    EXPECTED_NEW_APP="$new_app" \
    FAIL_ONCE_AT=${FAIL_ONCE_AT:-app-secret} \
        bash ${ROTATION_BASH_TRACE:+-x} "$script_dir/rotate-mysql-credentials.sh" "$rotation_file"
}

set +e
first_output=$(run_rotation 2>&1)
first_status=$?
set -e
[ "$first_status" -ne 0 ] || { printf '부분 실패 주입이 동작하지 않았습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/replicas")" = 0 ] || {
    printf '%s\n' "$first_output" >&2
    printf '실패 뒤 app이 중지되지 않았습니다.\n' >&2
    exit 1
}
[ "$(cat "$state_dir/db_root")" = "$new_root" ] || { printf 'DB root 회전 상태가 예상과 다릅니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/phase")" = db-updated ] || { printf '재개 단계가 기록되지 않았습니다.\n' >&2; exit 1; }
printf '%s' "$first_output" | grep -q 'app을 0 replica로 되돌리고' \
    || { printf '실패 시 app drain 결과가 보고되지 않았습니다.\n' >&2; exit 1; }

run_rotation >/dev/null
[ "$(cat "$state_dir/phase")" = completed ] || { printf '재개 후 완료 상태가 아닙니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/replicas")" = 1 ] || { printf '원래 app replica가 복구되지 않았습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/app_secret_password")" = "$new_app" ] \
    || { printf 'app Secret이 새 비밀번호로 복구되지 않았습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/alter_count")" = 1 ] || { printf '재개 과정에서 DB 계정을 다시 변경했습니다.\n' >&2; exit 1; }

run_rotation >/dev/null
[ "$(cat "$state_dir/alter_count")" = 1 ] || { printf '완료된 목표를 다시 실행했습니다.\n' >&2; exit 1; }

printf '%s' "$old_app" > "$state_dir/app_secret_password"
run_rotation >/dev/null
[ "$(cat "$state_dir/app_secret_password")" = "$new_app" ] \
    || { printf '완료 annotation 뒤 Secret drift를 복구하지 못했습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/alter_count")" = 1 ] || { printf 'Secret drift 복구 중 DB 계정을 다시 변경했습니다.\n' >&2; exit 1; }

printf '%s' secrets-updated > "$state_dir/phase"
printf '%s' 0 > "$state_dir/replicas"
set +e
completed_response_output=$(FAIL_ONCE_AT=completed-response run_rotation 2>&1)
completed_response_status=$?
set -e
[ "$completed_response_status" -ne 0 ] \
    || { printf '완료 annotation 응답 유실 주입이 동작하지 않았습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/phase")" = completed ] \
    || { printf '응답 유실 전에 완료 annotation이 적용되지 않았습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/replicas")" = 0 ] || {
    printf '%s\n' "$completed_response_output" >&2
    printf '완료 응답 유실 뒤 app drain이 유지되지 않았습니다.\n' >&2
    exit 1
}

run_rotation >/dev/null
[ "$(cat "$state_dir/replicas")" = 1 ] \
    || { printf '완료 annotation 재개에서 원래 app replica를 복구하지 못했습니다.\n' >&2; exit 1; }
[ "$(cat "$state_dir/alter_count")" = 1 ] \
    || { printf '완료 annotation 재개에서 DB 계정을 다시 변경했습니다.\n' >&2; exit 1; }

printf 'MySQL 자격증명 회전 부분 실패/재개 검증 완료\n'
