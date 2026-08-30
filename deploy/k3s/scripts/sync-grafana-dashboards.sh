#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

sync_dashboard() {
    source_file=$1
    generated_file=$2
    [ -f "$source_file" ] || die "Grafana 대시보드 원본을 찾을 수 없습니다: $source_file"

    case "${3:-}" in
        "")
            tmp=$(mktemp "${TMPDIR:-/tmp}/happygallery-grafana-dashboard.XXXXXX")
            cp "$source_file" "$tmp"
            chmod 644 "$tmp"
            mv "$tmp" "$generated_file"
            ;;
        --check)
            cmp -s "$source_file" "$generated_file" \
                || die "k3s Grafana 대시보드 산출물이 원본과 다릅니다. sync-grafana-dashboards.sh를 실행하세요."
            ;;
    esac
}

require_command cmp
mode=${1:-}
case "$mode" in
    ""|--check) ;;
    *) die "사용법: $0 [--check]" ;;
esac

sync_dashboard \
    "$REPO_ROOT/monitoring/dashboards/system.json" \
    "$BASE_DIR/grafana-system.generated.json" \
    "$mode"
sync_dashboard \
    "$REPO_ROOT/monitoring/dashboards/funnel.json" \
    "$BASE_DIR/grafana-funnel.generated.json" \
    "$mode"

[ "$mode" = --check ] \
    || info "k3s Grafana 대시보드 산출물을 갱신했습니다."
