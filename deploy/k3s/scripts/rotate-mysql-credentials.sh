#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <mysql-rotation.env>"
rotation_file=$1
require_command base64
validate_env_file "$rotation_file"
require_private_file "$rotation_file"
[ "${CONFIRM_MYSQL_CREDENTIAL_ROTATION:-}" = rotate-happygallery-mysql ] \
    || die "CONFIRM_MYSQL_CREDENTIAL_ROTATION=rotate-happygallery-mysql 를 지정해야 합니다."

new_root_password=$(require_env_value MYSQL_ROOT_PASSWORD "$rotation_file")
new_app_password=$(require_env_value MYSQL_PASSWORD "$rotation_file")
for password in "$new_root_password" "$new_app_password"; do
    printf '%s' "$password" | grep -Eq '^[A-Za-z0-9._~-]{32,128}$' \
        || die "새 MySQL 비밀번호는 32~128자의 영문/숫자/._~- 조합이어야 합니다."
done

fingerprint_input=$(mktemp "${TMPDIR:-/tmp}/happygallery-mysql-rotation.XXXXXX")
trap 'rm -f "$fingerprint_input"' EXIT HUP INT TERM
chmod 600 "$fingerprint_input"
printf '%s\n%s' "$new_root_password" "$new_app_password" > "$fingerprint_input"
target_fingerprint=$(sha256_file "$fingerprint_input")
rm -f "$fingerprint_input"
trap - EXIT HUP INT TERM

rotation_annotation() {
    case "$1" in
        phase) path='happygallery\.io/mysql-rotation-phase' ;;
        target) path='happygallery\.io/mysql-rotation-target' ;;
        replicas) path='happygallery\.io/mysql-rotation-original-replicas' ;;
        *) die "알 수 없는 MySQL rotation annotation입니다: $1" ;;
    esac
    kube -n "$NAMESPACE" get secret happygallery-mysql \
        -o "jsonpath={.metadata.annotations.$path}"
}

mysql_query() {
    mysql_username=$1
    mysql_password=$2
    mysql_sql=$3
    {
        printf '%s\n%s\n%s\n' "$mysql_username" "$mysql_password" "$mysql_sql"
    } | kube -n "$NAMESPACE" exec -i mysql-0 -- sh -ec '
        IFS= read -r mysql_username
        IFS= read -r mysql_password
        client_config=$(mktemp)
        trap '\''rm -f "$client_config"'\'' EXIT HUP INT TERM
        umask 077
        printf "[client]\nuser=%s\npassword=%s\n" \
            "$mysql_username" "$mysql_password" > "$client_config"
        mysql --defaults-extra-file="$client_config"
    '
}

root_password_works() {
    mysql_query root "$1" 'SELECT 1;' >/dev/null 2>&1
}

app_password_works() {
    mysql_query "$mysql_user" "$1" 'SELECT 1;' >/dev/null 2>&1
}

restore_app_if_needed() {
    [ "$original_replicas" -eq 1 ] || return 0
    current_replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
    if [ "${current_replicas:-0}" -ne 1 ]; then
        kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
    fi
    kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
}

kube -n "$NAMESPACE" get pvc data-mysql-0 >/dev/null \
    || die "기존 MySQL PVC를 찾을 수 없습니다. 최초 Secret 생성에는 create-secrets.sh를 사용하세요."
kube -n "$NAMESPACE" get secret happygallery-mysql >/dev/null
kube -n "$NAMESPACE" get secret happygallery-app >/dev/null
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null

mysql_user_encoded=$(kube -n "$NAMESPACE" get secret happygallery-mysql -o 'jsonpath={.data.MYSQL_USER}')
mysql_user=$(printf '%s' "$mysql_user_encoded" | base64 --decode)
current_root_encoded=$(kube -n "$NAMESPACE" get secret happygallery-mysql -o 'jsonpath={.data.MYSQL_ROOT_PASSWORD}')
current_root_password=$(printf '%s' "$current_root_encoded" | base64 --decode)
current_mysql_app_encoded=$(kube -n "$NAMESPACE" get secret happygallery-mysql -o 'jsonpath={.data.MYSQL_PASSWORD}')
current_mysql_app_password=$(printf '%s' "$current_mysql_app_encoded" | base64 --decode)
current_app_db_encoded=$(kube -n "$NAMESPACE" get secret happygallery-app -o 'jsonpath={.data.DB_PASSWORD}')
current_app_db_password=$(printf '%s' "$current_app_db_encoded" | base64 --decode)
printf '%s' "$mysql_user" | grep -Eq '^[A-Za-z][A-Za-z0-9_]{0,31}$' \
    || die "기존 MYSQL_USER가 안전하게 회전할 수 있는 형식이 아닙니다."

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -le 1 ] || die "단일 app replica 구성에서만 MySQL 자격증명을 회전할 수 있습니다."

phase=$(rotation_annotation phase)
recorded_target=$(rotation_annotation target)
case "$phase" in
    started|db-updated|secrets-updated)
        [ "$recorded_target" = "$target_fingerprint" ] \
            || die "진행 중인 MySQL 회전 목표가 다릅니다. 기존 회전 파일로 재개하세요."
        original_replicas=$(rotation_annotation replicas)
        case "$original_replicas" in 0|1) ;; *) die "회전 전 app replica annotation이 올바르지 않습니다." ;; esac
        info "중단된 MySQL 자격증명 회전을 재개합니다 [phase=$phase]."
        ;;
    ''|completed)
        if [ "$phase" = completed ] && [ "$recorded_target" = "$target_fingerprint" ]; then
            if [ "$current_root_password" = "$new_root_password" ] \
                && [ "$current_mysql_app_password" = "$new_app_password" ] \
                && [ "$current_app_db_password" = "$new_app_password" ] \
                && root_password_works "$new_root_password" \
                && app_password_works "$new_app_password"; then
                original_replicas=$(rotation_annotation replicas)
                case "$original_replicas" in 0|1) ;; *) die "회전 전 app replica annotation이 올바르지 않습니다." ;; esac
                restore_app_if_needed
                info "같은 목표의 MySQL 자격증명 회전이 완료됐고 DB와 Secret이 일치합니다."
                exit 0
            fi
            info "완료 기록과 실제 DB/Secret 상태가 달라 같은 목표로 복구합니다."
        fi
        original_replicas=${replicas:-0}
        printf '{"metadata":{"annotations":{"happygallery.io/mysql-rotation-phase":"started","happygallery.io/mysql-rotation-target":"%s","happygallery.io/mysql-rotation-original-replicas":"%s"}}}' \
            "$target_fingerprint" "$original_replicas" \
            | kube -n "$NAMESPACE" patch secret happygallery-mysql \
                --type merge --patch-file=/dev/stdin >/dev/null
        ;;
    *) die "알 수 없는 MySQL 자격증명 회전 상태입니다: $phase" ;;
esac

rotation_started=false
on_rotation_exit() {
    status=$?
    trap - EXIT HUP INT TERM
    if [ "$rotation_started" = true ]; then
        if kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null \
            && (wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120); then
            printf '%s\n' "오류: MySQL 자격증명 회전 실패. app을 0 replica로 되돌리고 Pod 종료를 확인했습니다." >&2
        else
            printf '%s\n' "치명적 오류: MySQL 자격증명 회전 실패 후 app 중지를 확인하지 못했습니다. 즉시 deployment/app 상태를 확인하세요." >&2
        fi
    fi
    exit "$status"
}

info "DB 연결을 끊기 위해 app을 0 replica로 축소합니다."
trap on_rotation_exit EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM
rotation_started=true
kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

if root_password_works "$new_root_password"; then
    if app_password_works "$new_app_password"; then
        info "DB 계정은 이미 새 자격증명을 사용합니다. Secret 갱신 단계부터 재개합니다."
    else
        info "MySQL 애플리케이션 계정 비밀번호를 새 목표로 복구합니다."
        alter_app_sql=$(printf "ALTER USER '%s'@'%%' IDENTIFIED BY '%s';" \
            "$mysql_user" "$new_app_password")
        mysql_query root "$new_root_password" "$alter_app_sql"
    fi
elif root_password_works "$current_root_password"; then
    info "MySQL root와 애플리케이션 계정 비밀번호를 한 문장으로 회전합니다."
    alter_sql=$(printf "ALTER USER 'root'@'localhost' IDENTIFIED BY '%s', '%s'@'%%' IDENTIFIED BY '%s';" \
        "$new_root_password" "$mysql_user" "$new_app_password")
    mysql_query root "$current_root_password" "$alter_sql"
else
    die "기존 Secret과 새 회전 파일의 root 비밀번호 모두 DB에 연결되지 않습니다. app을 중지한 채 자격증명을 확인하세요."
fi

root_password_works "$new_root_password" \
    || die "새 root 비밀번호로 DB 연결을 확인할 수 없습니다."
app_password_works "$new_app_password" \
    || die "새 애플리케이션 비밀번호로 DB 연결을 확인할 수 없습니다."

printf '{"metadata":{"annotations":{"happygallery.io/mysql-rotation-phase":"db-updated","happygallery.io/mysql-rotation-target":"%s","happygallery.io/mysql-rotation-original-replicas":"%s"}}}' \
    "$target_fingerprint" "$original_replicas" \
    | kube -n "$NAMESPACE" patch secret happygallery-mysql \
        --type merge --patch-file=/dev/stdin >/dev/null

root_encoded=$(base64_value "$new_root_password")
app_encoded=$(base64_value "$new_app_password")
printf '{"data":{"MYSQL_ROOT_PASSWORD":"%s","MYSQL_PASSWORD":"%s"}}' \
    "$root_encoded" "$app_encoded" \
    | kube -n "$NAMESPACE" patch secret happygallery-mysql --type merge --patch-file=/dev/stdin >/dev/null
printf '{"data":{"DB_PASSWORD":"%s"}}' "$app_encoded" \
    | kube -n "$NAMESPACE" patch secret happygallery-app --type merge --patch-file=/dev/stdin >/dev/null
printf '{"metadata":{"annotations":{"happygallery.io/mysql-rotation-phase":"secrets-updated","happygallery.io/mysql-rotation-target":"%s","happygallery.io/mysql-rotation-original-replicas":"%s"}}}' \
    "$target_fingerprint" "$original_replicas" \
    | kube -n "$NAMESPACE" patch secret happygallery-mysql \
        --type merge --patch-file=/dev/stdin >/dev/null

info "새 Secret을 읽도록 MySQL을 재시작합니다."
kube -n "$NAMESPACE" rollout restart statefulset/mysql >/dev/null
kube -n "$NAMESPACE" rollout status statefulset/mysql --timeout=5m
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null
root_password_works "$new_root_password" \
    || die "MySQL 재시작 후 새 root 비밀번호 검증에 실패했습니다."
app_password_works "$new_app_password" \
    || die "MySQL 재시작 후 새 애플리케이션 비밀번호 검증에 실패했습니다."

restore_app_if_needed

printf '{"metadata":{"annotations":{"happygallery.io/mysql-rotation-phase":"completed","happygallery.io/mysql-rotation-target":"%s","happygallery.io/mysql-rotation-original-replicas":"%s","happygallery.io/mysql-rotation-completed-at-epoch":"%s"}}}' \
    "$target_fingerprint" "$original_replicas" "$(date +%s)" \
    | kube -n "$NAMESPACE" patch secret happygallery-mysql \
        --type merge --patch-file=/dev/stdin >/dev/null

rotation_started=false
trap - EXIT HUP INT TERM
info "MySQL root/애플리케이션 자격증명 회전 완료"
