#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <공개 DNS 이름>"
public_host=$1
require_command curl

kube get node >/dev/null
traefik_pods=$(kube -n kube-system get pods \
    -l app.kubernetes.io/name=traefik --no-headers 2>/dev/null | awk 'END { print NR }')
[ "$traefik_pods" -ge 1 ] || die "허용 대상으로 지정한 Traefik Pod label을 찾을 수 없습니다."
kube -n "$NAMESPACE" get pods
kube -n "$NAMESPACE" get pvc data-mysql-0 -o jsonpath='{.status.phase}' | grep -qx Bound \
    || die "MySQL PVC가 Bound 상태가 아닙니다."
for pvc in app-media prometheus-data alertmanager-data grafana-data; do
    kube -n "$NAMESPACE" get pvc "$pvc" -o jsonpath='{.status.phase}' | grep -qx Bound \
        || die "$pvc PVC가 Bound 상태가 아닙니다."
done

for workload in statefulset/mysql deployment/redis deployment/app deployment/frontend deployment/prometheus deployment/alertmanager deployment/grafana; do
    ready=$(kube -n "$NAMESPACE" get "$workload" -o jsonpath='{.status.readyReplicas}')
    [ "${ready:-0}" -eq 1 ] || die "$workload ready replica가 1이 아닙니다."
done

if kube -n "$NAMESPACE" get service -o jsonpath='{range .items[*]}{.spec.type}{"\n"}{end}' \
    | grep -Eq 'NodePort|LoadBalancer'; then
    die "happygallery namespace에 직접 공개 Service가 있습니다."
fi

port=${LOCAL_MANAGEMENT_PORT:-18081}
prometheus_port=${LOCAL_PROMETHEUS_PORT:-19090}
log_file=$(mktemp "${TMPDIR:-/tmp}/happygallery-port-forward.XXXXXX")
prometheus_log_file=$(mktemp "${TMPDIR:-/tmp}/happygallery-prometheus-port-forward.XXXXXX")
public_check_dir=$(mktemp -d "${TMPDIR:-/tmp}/happygallery-public-check.XXXXXX")
kube -n "$NAMESPACE" port-forward service/app-management "$port:8081" >"$log_file" 2>&1 &
port_forward_pid=$!
kube -n "$NAMESPACE" port-forward service/prometheus "$prometheus_port:9090" >"$prometheus_log_file" 2>&1 &
prometheus_port_forward_pid=$!
cleanup() {
    kill "$port_forward_pid" >/dev/null 2>&1 || true
    kill "$prometheus_port_forward_pid" >/dev/null 2>&1 || true
    wait "$port_forward_pid" >/dev/null 2>&1 || true
    wait "$prometheus_port_forward_pid" >/dev/null 2>&1 || true
    rm -f "$log_file" "$prometheus_log_file"
    rm -rf "$public_check_dir"
}
trap cleanup EXIT HUP INT TERM

attempt=0
until curl -fsS "http://127.0.0.1:$port/actuator/health/readiness" | grep -q '"status":"UP"'; do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 20 ] || {
        cat "$log_file" >&2
        die "내부 Actuator readiness 확인에 실패했습니다."
    }
    sleep 1
done

attempt=0
until prometheus_target=$(curl -fsS "http://127.0.0.1:$prometheus_port/api/v1/targets?state=active"); do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 20 ] || {
        cat "$prometheus_log_file" >&2
        die "Prometheus target API 확인에 실패했습니다."
    }
    sleep 1
done
printf '%s' "$prometheus_target" | grep -q 'app-management:8081' \
    || die "Prometheus가 app-management:8081 관리 포트를 대상으로 사용하지 않습니다."
printf '%s' "$prometheus_target" | grep -q '"health":"up"' \
    || die "Prometheus happygallery scrape target이 UP이 아닙니다."
prometheus_alertmanagers=$(curl -fsS "http://127.0.0.1:$prometheus_port/api/v1/alertmanagers")
printf '%s' "$prometheus_alertmanagers" | grep -q 'alertmanager:9093' \
    || die "Prometheus에 Alertmanager 대상이 없습니다."
printf '%s' "$prometheus_alertmanagers" | grep -q 'activeAlertmanagers.*alertmanager:9093' \
    || die "Prometheus가 Alertmanager를 활성 대상으로 인식하지 못했습니다."

if [ "${SKIP_PUBLIC_CHECK:-false}" = true ]; then
    info "SKIP_PUBLIC_CHECK=true: DNS/TLS 공개 경로 검증을 건너뜁니다."
    exit 0
fi

http_code=$(curl -sS -o /dev/null -w '%{http_code}' "http://$public_host/")
case "$http_code" in
    301|302|307|308) ;;
    *) die "HTTP 요청이 HTTPS로 redirect되지 않았습니다: $http_code" ;;
esac

root_code=$(curl -sS -D "$public_check_dir/root.headers" -o "$public_check_dir/root.body" \
    -w '%{http_code}' "https://$public_host/")
[ "$root_code" -eq 200 ] || die "루트 SSR 문서가 200을 반환하지 않았습니다: $root_code"
grep -qi '^content-type: text/html' "$public_check_dir/root.headers" \
    || die "루트 SSR 응답이 HTML이 아닙니다."
grep -Fq "<link rel=\"canonical\" href=\"https://$public_host/\"" "$public_check_dir/root.body" \
    || die "루트 SSR HTML에 대표 origin canonical이 없습니다."
grep -Eq '<h1[^>]*>[^<]*해피갤러리[^<]*</h1>' "$public_check_dir/root.body" \
    || die "루트 SSR HTML에 해피갤러리 H1 본문이 없습니다."
csp_header=$(grep -i '^content-security-policy-report-only:' "$public_check_dir/root.headers" | head -n 1 | tr -d '\r')
[ -n "$csp_header" ] || die "루트 SSR 응답에 CSP Report-Only 헤더가 없습니다."
csp_nonce=$(printf '%s' "$csp_header" | sed -n "s/.*'nonce-\([^']*\)'.*/\1/p")
[ -n "$csp_nonce" ] || die "CSP Report-Only 헤더에 요청별 nonce가 없습니다."
grep -Fq "nonce=\"$csp_nonce\"" "$public_check_dir/root.body" \
    || die "CSP 헤더 nonce와 SSR inline script nonce가 다릅니다."

robots_code=$(curl -sS -D "$public_check_dir/robots.headers" -o "$public_check_dir/robots.body" \
    -w '%{http_code}' "https://$public_host/robots.txt")
[ "$robots_code" -eq 200 ] || die "robots.txt가 200을 반환하지 않았습니다: $robots_code"
grep -qi '^content-type: text/plain' "$public_check_dir/robots.headers" \
    || die "robots.txt Content-Type이 text/plain이 아닙니다."
grep -Fqx "Sitemap: https://$public_host/sitemap.xml" "$public_check_dir/robots.body" \
    || die "robots.txt에 대표 origin sitemap 선언이 없습니다."

sitemap_code=$(curl -sS -D "$public_check_dir/sitemap.headers" -o "$public_check_dir/sitemap.body" \
    -w '%{http_code}' "https://$public_host/sitemap.xml")
[ "$sitemap_code" -eq 200 ] || die "sitemap.xml이 200을 반환하지 않았습니다: $sitemap_code"
grep -qi '^content-type: application/xml' "$public_check_dir/sitemap.headers" \
    || die "sitemap.xml Content-Type이 application/xml이 아닙니다."
grep -Fq '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' "$public_check_dir/sitemap.body" \
    || die "sitemap.xml에 표준 urlset root가 없습니다."
grep -Fq "<loc>https://$public_host/</loc>" "$public_check_dir/sitemap.body" \
    || die "sitemap.xml에 대표 루트 URL이 없습니다."

not_found_code=$(curl -sS -D "$public_check_dir/not-found.headers" -o "$public_check_dir/not-found.body" \
    -w '%{http_code}' "https://$public_host/__happygallery_verify_not_found__")
[ "$not_found_code" -eq 404 ] || die "알 수 없는 SSR route가 404를 반환하지 않았습니다: $not_found_code"
grep -qi '^content-type: text/html' "$public_check_dir/not-found.headers" \
    || die "알 수 없는 SSR route가 HTML 404를 반환하지 않았습니다."

api_code=$(curl -sS -D "$public_check_dir/api.headers" -o "$public_check_dir/api.body" \
    -w '%{http_code}' "https://$public_host/api/v1/definitely-not-a-route")
[ "$api_code" -eq 404 ] || die "알 수 없는 API 경로가 404를 반환하지 않았습니다: $api_code"
grep -qi '^content-type: application/json' "$public_check_dir/api.headers" \
    || die "알 수 없는 API 경로가 JSON이 아닌 응답을 반환했습니다."
if grep -qi '<!doctype html' "$public_check_dir/api.body"; then
    die "API 오류가 frontend SSR HTML로 치환됐습니다."
fi

info "Pod/PVC/Actuator/HTTP redirect/TLS/SSR SEO/API 경계 검증 완료"
