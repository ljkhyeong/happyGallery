#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -ge 1 ] || die "사용법: $0 enable <admin-setup.env> | disable"
action=$1

case "$action" in
    enable)
        [ "$#" -eq 2 ] || die "사용법: $0 enable <admin-setup.env>"
        setup_file=$2
        validate_env_file "$setup_file"
        require_private_file "$setup_file"
        require_env_value ADMIN_SETUP_TOKEN "$setup_file" >/dev/null
        kube create secret generic happygallery-admin-setup \
            --namespace "$NAMESPACE" \
            --from-env-file="$setup_file" \
            --dry-run=client -o yaml | kube apply -f - >/dev/null
        kube -n "$NAMESPACE" set env deployment/app --from=secret/happygallery-admin-setup >/dev/null
        kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
        info "관리자 최초 설정 토큰을 임시 활성화했습니다. 설정 직후 disable을 실행하세요."
        ;;
    disable)
        [ "$#" -eq 1 ] || die "사용법: $0 disable"
        kube -n "$NAMESPACE" set env deployment/app ADMIN_SETUP_TOKEN- >/dev/null
        kube -n "$NAMESPACE" delete secret happygallery-admin-setup --ignore-not-found >/dev/null
        kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
        info "관리자 최초 설정 토큰과 Pod 환경 변수를 제거했습니다."
        ;;
    *)
        die "알 수 없는 동작입니다: $action"
        ;;
esac
