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

for workload in statefulset/mysql deployment/redis deployment/app deployment/frontend deployment/prometheus; do
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
kube -n "$NAMESPACE" port-forward service/app-management "$port:8081" >"$log_file" 2>&1 &
port_forward_pid=$!
kube -n "$NAMESPACE" port-forward service/prometheus "$prometheus_port:9090" >"$prometheus_log_file" 2>&1 &
prometheus_port_forward_pid=$!
cleanup() {
    kill "$port_forward_pid" >/dev/null 2>&1 || true
    kill "$prometheus_port_forward_pid" >/dev/null 2>&1 || true
    wait "$port_forward_pid" >/dev/null 2>&1 || true
    wait "$prometheus_port_forward_pid" >/dev/null 2>&1 || true
    rm -f "$log_file" "$prometheus_log_file" "$log_file.headers" "$log_file.body"
}
trap cleanup EXIT HUP INT TERM

attempt=0
until curl -fsS "http://127.0.0.1:$port/actuator/health" | grep -q '"status":"UP"'; do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 20 ] || {
        cat "$log_file" >&2
        die "내부 Actuator health 확인에 실패했습니다."
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

if [ "${SKIP_PUBLIC_CHECK:-false}" = true ]; then
    info "SKIP_PUBLIC_CHECK=true: DNS/TLS 공개 경로 검증을 건너뜁니다."
    exit 0
fi

http_code=$(curl -sS -o /dev/null -w '%{http_code}' "http://$public_host/")
case "$http_code" in
    301|302|307|308) ;;
    *) die "HTTP 요청이 HTTPS로 redirect되지 않았습니다: $http_code" ;;
esac

curl -fsS "https://$public_host/" >/dev/null
curl -sS -D "$log_file.headers" -o "$log_file.body" \
    "https://$public_host/api/v1/definitely-not-a-route"
grep -qi '^content-type: application/json' "$log_file.headers" \
    || die "알 수 없는 API 경로가 JSON이 아닌 응답을 반환했습니다."
if grep -qi '<!doctype html' "$log_file.body"; then
    die "API 오류가 SPA index.html로 치환됐습니다."
fi

info "Pod/PVC/Actuator/HTTP redirect/TLS/API-SPA 경계 검증 완료"
