#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 2 ] || die "사용법: $0 <app.env> <alertmanager.env|alert-webhook-url>"
require_command ruby
umask 077
config_dir=$(mktemp -d "${TMPDIR:-/tmp}/happygallery-alertmanager.XXXXXX")
trap 'rm -rf "$config_dir"' EXIT
ruby "$SCRIPT_DIR/alert-delivery.rb" render "$1" "$2" "$config_dir"
kube apply -f "$BASE_DIR/namespace.yaml" >/dev/null
kube create secret generic happygallery-alertmanager \
    --namespace "$NAMESPACE" --from-file="$config_dir" \
    --dry-run=client -o yaml | kube apply -f - >/dev/null
info "Alertmanager 설정과 발송 자격 증명을 Secret에 저장했습니다. 값은 출력하지 않았습니다."
info "이미 실행 중이라면 Alertmanager를 재시작해야 변경이 반영됩니다."
