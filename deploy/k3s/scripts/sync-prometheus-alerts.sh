#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

source_file="$REPO_ROOT/monitoring/alerts.yml"
generated_file="$BASE_DIR/prometheus-alerts.generated.yml"

[ -f "$source_file" ] || die "Prometheus 경보 원본을 찾을 수 없습니다: $source_file"
require_command cmp

case "${1:-}" in
    "")
        tmp=$(mktemp "${TMPDIR:-/tmp}/happygallery-prometheus-alerts.XXXXXX")
        trap 'rm -f "$tmp"' EXIT HUP INT TERM
        cp "$source_file" "$tmp"
        chmod 644 "$tmp"
        mv "$tmp" "$generated_file"
        trap - EXIT HUP INT TERM
        info "k3s Prometheus 경보 산출물을 갱신했습니다: $generated_file"
        ;;
    --check)
        cmp -s "$source_file" "$generated_file" \
            || die "k3s Prometheus 경보 산출물이 원본과 다릅니다. sync-prometheus-alerts.sh를 실행하세요."
        ;;
    *)
        die "사용법: $0 [--check]"
        ;;
esac
