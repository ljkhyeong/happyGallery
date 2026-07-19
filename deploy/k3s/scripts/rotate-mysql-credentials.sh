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

kube -n "$NAMESPACE" get pvc data-mysql-0 >/dev/null \
    || die "기존 MySQL PVC를 찾을 수 없습니다. 최초 Secret 생성에는 create-secrets.sh를 사용하세요."
kube -n "$NAMESPACE" get secret happygallery-mysql >/dev/null
kube -n "$NAMESPACE" get secret happygallery-app >/dev/null
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null

mysql_user_encoded=$(kube -n "$NAMESPACE" get secret happygallery-mysql -o 'jsonpath={.data.MYSQL_USER}')
mysql_user=$(printf '%s' "$mysql_user_encoded" | base64 --decode)
printf '%s' "$mysql_user" | grep -Eq '^[A-Za-z][A-Za-z0-9_]{0,31}$' \
    || die "기존 MYSQL_USER가 안전하게 회전할 수 있는 형식이 아닙니다."

replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
[ "${replicas:-0}" -le 1 ] || die "단일 app replica 구성에서만 MySQL 자격증명을 회전할 수 있습니다."

rotation_started=false
on_rotation_error() {
    status=$?
    trap - ERR
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
rotation_started=true
trap on_rotation_error ERR
kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null
wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120

info "MySQL root와 애플리케이션 계정 비밀번호를 한 문장으로 회전합니다."
printf "ALTER USER 'root'@'localhost' IDENTIFIED BY '%s', '%s'@'%%' IDENTIFIED BY '%s';\n" \
    "$new_root_password" "$mysql_user" "$new_app_password" \
    | kube -n "$NAMESPACE" exec -i mysql-0 -- sh -ec \
        'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'

root_encoded=$(base64_value "$new_root_password")
app_encoded=$(base64_value "$new_app_password")
printf '{"data":{"MYSQL_ROOT_PASSWORD":"%s","MYSQL_PASSWORD":"%s"}}' \
    "$root_encoded" "$app_encoded" \
    | kube -n "$NAMESPACE" patch secret happygallery-mysql --type merge --patch-file=/dev/stdin >/dev/null
printf '{"data":{"DB_PASSWORD":"%s"}}' "$app_encoded" \
    | kube -n "$NAMESPACE" patch secret happygallery-app --type merge --patch-file=/dev/stdin >/dev/null

info "새 Secret을 읽도록 MySQL을 재시작합니다."
kube -n "$NAMESPACE" rollout restart statefulset/mysql >/dev/null
kube -n "$NAMESPACE" rollout status statefulset/mysql --timeout=5m

if [ "${replicas:-0}" -eq 1 ]; then
    kube -n "$NAMESPACE" scale deployment/app --replicas=1 >/dev/null
    kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
fi

rotation_started=false
trap - ERR
info "MySQL root/애플리케이션 자격증명 회전 완료"
