#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

require_command ruby
require_command grep

"$SCRIPT_DIR/sync-prometheus-alerts.sh" --check
"$SCRIPT_DIR/sync-grafana-dashboards.sh" --check

tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/happygallery-k3s-validate.XXXXXX")
trap 'rm -rf "$tmp_dir"' EXIT HUP INT TERM
rendered="$tmp_dir/rendered.yaml"

PUBLIC_HOST=gallery.example.com \
ACME_EMAIL=ops@example.com \
APP_IMAGE=localhost/happygallery-app:0123456789abcdef0123456789abcdef01234567 \
FRONTEND_IMAGE=localhost/happygallery-frontend:0123456789abcdef0123456789abcdef01234567 \
APP_IMAGE_DIGEST=sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
FRONTEND_IMAGE_DIGEST=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb \
IMAGE_TAG=0123456789abcdef0123456789abcdef01234567 \
    "$SCRIPT_DIR/render-manifests.sh" "$rendered"

runtime_references="$tmp_dir/runtime-image-references.tsv"
runtime_metadata="$tmp_dir/runtime-images.env"
runtime_inventory="$tmp_dir/runtime-image-inventory.tsv"
ruby "$SCRIPT_DIR/runtime-images-from-manifest.rb" \
    --references "$rendered" > "$runtime_references"
[ -s "$runtime_references" ] || die "release manifest에서 runtime image 참조를 추출하지 못했습니다."

tab=$(printf '\t')
while IFS="$tab" read -r runtime_key runtime_image unexpected; do
    [ -n "$runtime_key" ] && [ -n "$runtime_image" ] && [ -z "$unexpected" ] \
        || die "runtime image 참조 inventory 형식이 올바르지 않습니다."
    printf '%s' "$runtime_key" | grep -Eq '^[A-Z][A-Z0-9_]*$' \
        || die "runtime image key 형식이 올바르지 않습니다: $runtime_key"
    printf '%s_IMAGE=%s\n%s_IMAGE_DIGEST=sha256:%064d\n' \
        "$runtime_key" "$runtime_image" "$runtime_key" 0
done < "$runtime_references" > "$runtime_metadata"
validate_env_file "$runtime_metadata"
ruby "$SCRIPT_DIR/runtime-images-from-manifest.rb" \
    --inventory "$rendered" "$runtime_metadata" > "$runtime_inventory"

ruby -e '
  require "yaml"

  references = File.readlines(ARGV.fetch(1), chomp: true).map { |line| line.split("\t", -1) }
  inventory = File.readlines(ARGV.fetch(2), chomp: true).map { |line| line.split("\t", -1) }
  abort "runtime image 참조 key가 중복됐습니다." unless references.map(&:first).uniq.size == references.size
  abort "runtime image inventory가 참조와 다릅니다." unless
    inventory.map { |key, image, _digest| [key, image] } == references
  abort "runtime image inventory digest가 올바르지 않습니다." unless inventory.all? do |_key, _image, digest|
    digest&.match?(/\Asha256:[a-f0-9]{64}\z/)
  end

  documents = YAML.load_stream(File.read(ARGV.fetch(0))).compact
  manifest_runtime_images = documents.each_with_object([]) do |document, images|
    next unless %w[Deployment StatefulSet].include?(document["kind"])
    next if %w[app frontend].include?(document.dig("metadata", "name"))

    containers = document.dig("spec", "template", "spec", "containers") || []
    images.concat(containers.map { |container| container["image"] })
  end
  abort "manifest parser가 runtime workload image를 빠뜨렸습니다." unless
    references.map(&:last).sort == manifest_runtime_images.sort
' "$rendered" "$runtime_references" "$runtime_inventory"

invalid_runtime_metadata="$tmp_dir/invalid-runtime-images.env"
cp "$runtime_metadata" "$invalid_runtime_metadata"
printf 'UNKNOWN_IMAGE=example.invalid/unknown:1\n' >> "$invalid_runtime_metadata"
if ruby "$SCRIPT_DIR/runtime-images-from-manifest.rb" \
    --inventory "$rendered" "$invalid_runtime_metadata" >/dev/null 2>&1; then
    die "runtime image parser가 알 수 없는 metadata key를 허용합니다."
fi

ruby -e '
  require "yaml"
  documents = YAML.load_stream(File.read(ARGV.fetch(0)))
  abort "빈 Kubernetes 문서가 있습니다." if documents.any?(&:nil?)
  abort "apiVersion/kind가 없는 문서가 있습니다." unless documents.all? { |d| d["apiVersion"] && d["kind"] }
  abort "평문 Secret manifest를 만들 수 없습니다." if documents.any? { |d| d["kind"] == "Secret" }
  alert_config = documents.find { |d| d["kind"] == "ConfigMap" && d.dig("metadata", "name") == "prometheus-alerts" }
  canonical_alerts = File.read(ARGV.fetch(1))
  abort "k3s Prometheus 경보가 canonical 원본과 다릅니다." unless alert_config&.dig("data", "alerts.yml") == canonical_alerts
  dashboard_config = documents.find { |d| d["kind"] == "ConfigMap" && d.dig("metadata", "name") == "grafana-dashboards" }
  canonical_system_dashboard = File.read(ARGV.fetch(2))
  canonical_funnel_dashboard = File.read(ARGV.fetch(3))
  abort "k3s Grafana system 대시보드가 canonical 원본과 다릅니다." unless
    dashboard_config&.dig("data", "system.json") == canonical_system_dashboard
  abort "k3s Grafana funnel 대시보드가 canonical 원본과 다릅니다." unless
    dashboard_config&.dig("data", "funnel.json") == canonical_funnel_dashboard
  ingresses = documents.select { |d| d["kind"] == "Ingress" }
  ingress_headers = documents.select { |d| d["kind"] == "Middleware" }
                             .flat_map { |d| d.dig("spec", "headers", "customResponseHeaders")&.keys || [] }
  abort "CSP는 frontend Nginx 한 곳에서만 설정해야 합니다." if ingress_headers.any? do |name|
    name.downcase.start_with?("content-security-policy")
  end
  paths = ingresses.flat_map { |d| d.dig("spec", "rules") || [] }
                   .flat_map { |r| r.dig("http", "paths") || [] }
  abort "Ingress가 Actuator를 외부에 노출합니다." if paths.any? { |p| p.fetch("path").start_with?("/actuator") }
  app_paths = paths.select { |p| p.dig("backend", "service", "name") == "app" }
  abort "Ingress에 API app backend가 없습니다." if app_paths.empty?
  abort "Ingress의 app backend는 8080 http port만 사용해야 합니다." unless app_paths.all? { |p| p.dig("backend", "service", "port", "name") == "http" }
  services = documents.select { |d| d["kind"] == "Service" }
  abort "직접 공개 Service가 있습니다." if services.any? { |d| %w[NodePort LoadBalancer].include?(d.dig("spec", "type")) }
  public_app = services.find { |d| d.dig("metadata", "name") == "app" }
  abort "app Service는 8080만 제공해야 합니다." unless public_app&.dig("spec", "ports")&.map { |p| p["port"] } == [8080]
  management_app = services.find { |d| d.dig("metadata", "name") == "app-management" }
  abort "app-management Service는 8081만 제공해야 합니다." unless management_app&.dig("spec", "ports")&.map { |p| p["port"] } == [8081]
  release_deployments = documents.select { |d| d["kind"] == "Deployment" && %w[app frontend].include?(d.dig("metadata", "name")) }
  abort "app/frontend 이미지는 sha256 digest로 고정해야 합니다." unless release_deployments.size == 2 && release_deployments.all? do |d|
    d.dig("spec", "template", "spec", "containers", 0, "image")&.match?(/@sha256:[a-f0-9]{64}\z/)
  end
  media_pvc = documents.find { |d| d["kind"] == "PersistentVolumeClaim" && d.dig("metadata", "name") == "app-media" }
  abort "app-media PVC는 local-path-retain 5Gi ReadWriteOnce여야 합니다." unless media_pvc &&
    media_pvc.dig("spec", "storageClassName") == "local-path-retain" &&
    media_pvc.dig("spec", "accessModes") == ["ReadWriteOnce"] &&
    media_pvc.dig("spec", "resources", "requests", "storage") == "5Gi"
  expected_monitoring_pvcs = {
    "prometheus-data" => "5Gi",
    "alertmanager-data" => "1Gi",
    "grafana-data" => "2Gi"
  }
  expected_monitoring_pvcs.each do |name, storage|
    pvc = documents.find { |d| d["kind"] == "PersistentVolumeClaim" && d.dig("metadata", "name") == name }
    abort "#{name} PVC가 local-path-retain #{storage} ReadWriteOnce가 아닙니다." unless pvc &&
      pvc.dig("spec", "storageClassName") == "local-path-retain" &&
      pvc.dig("spec", "accessModes") == ["ReadWriteOnce"] &&
      pvc.dig("spec", "resources", "requests", "storage") == storage
  end
  app_deployment = release_deployments.find { |d| d.dig("metadata", "name") == "app" }
  app_container = app_deployment&.dig("spec", "template", "spec", "containers", 0)
  app_config = documents.find { |d| d["kind"] == "ConfigMap" && d.dig("metadata", "name") == "app-config" }
  token_ttls = app_config&.fetch("data", {})&.slice(
    "GUEST_TOKEN_EXPIRY_HOURS",
    "GUEST_TOKEN_RECOVERY_EXPIRY_HOURS"
  )
  abort "비회원 토큰 TTL 기준이 app-config에 고정되지 않았습니다." unless token_ttls == {
    "GUEST_TOKEN_EXPIRY_HOURS" => "720",
    "GUEST_TOKEN_RECOVERY_EXPIRY_HOURS" => "24"
  }
  media_mount = app_container&.fetch("volumeMounts", [])&.find { |mount| mount["name"] == "media" }
  media_volume = app_deployment&.dig("spec", "template", "spec", "volumes")&.find { |volume| volume["name"] == "media" }
  abort "app-media PVC가 app의 미디어 저장 경로에 연결되지 않았습니다." unless
    media_mount&.dig("mountPath") == "/var/lib/happygallery/media" &&
    media_volume&.dig("persistentVolumeClaim", "claimName") == "app-media"
  singleton_deployments = documents.select do |d|
    d["kind"] == "Deployment" && %w[redis prometheus alertmanager grafana].include?(d.dig("metadata", "name"))
  end
  abort "상태를 가진 단일 인스턴스 Deployment는 Recreate 전략이어야 합니다." unless singleton_deployments.size == 4 && singleton_deployments.all? do |d|
    d.dig("spec", "replicas") == 1 && d.dig("spec", "strategy", "type") == "Recreate"
  end
  grafana = singleton_deployments.find { |d| d.dig("metadata", "name") == "grafana" }
  grafana_env = grafana.dig("spec", "template", "spec", "containers", 0, "env").to_h do |entry|
    [entry.fetch("name"), entry["value"]]
  end
  abort "Grafana는 cluster 내부 Viewer 전용이어야 합니다." unless
    grafana_env["GF_AUTH_ANONYMOUS_ENABLED"] == "true" &&
    grafana_env["GF_AUTH_ANONYMOUS_ORG_ROLE"] == "Viewer" &&
    grafana_env["GF_AUTH_DISABLE_LOGIN_FORM"] == "true"
  cluster_scoped = documents.select { |d| %w[Namespace StorageClass ClusterIssuer].include?(d["kind"]) }
  abort "cluster-scoped 리소스에 namespace가 있습니다." if cluster_scoped.any? { |d| d.dig("metadata", "namespace") }
  policies = documents.select { |d| d["kind"] == "NetworkPolicy" }.to_h { |d| [d.dig("metadata", "name"), d] }
  %w[allow-ingress-to-app allow-ingress-to-frontend allow-acme-http01-solver].each do |name|
    peers = policies.fetch(name).dig("spec", "ingress").flat_map { |rule| rule["from"] || [] }
    kube_system_peers = peers.select do |peer|
      peer.dig("namespaceSelector", "matchLabels", "kubernetes.io/metadata.name") == "kube-system"
    end
    exact = !kube_system_peers.empty? && kube_system_peers.all? do |peer|
      peer.dig("podSelector", "matchLabels", "app.kubernetes.io/name") == "traefik"
    end
    abort "#{name}이 kube-system 전체 Pod를 허용합니다." unless exact
  end
  mysql_policy = policies.fetch("allow-app-to-mysql")
  mysql_rules = mysql_policy.dig("spec", "ingress") || []
  mysql_peers = mysql_rules.flat_map { |rule| rule["from"] || [] }
  mysql_names = mysql_peers.map { |peer| peer.dig("podSelector", "matchLabels", "app.kubernetes.io/name") }.compact.sort
  abort "MySQL ingress는 app/key-rotation Pod만 허용해야 합니다." unless mysql_names == %w[app key-rotation]
  mysql_ports = mysql_rules.flat_map { |rule| rule["ports"] || [] }.map { |port| port["port"] }.uniq
  abort "MySQL ingress는 3306만 허용해야 합니다." unless mysql_ports == [3306]
  redis_policy = policies.fetch("allow-app-to-redis")
  redis_rules = redis_policy.dig("spec", "ingress") || []
  redis_peers = redis_rules.flat_map { |rule| rule["from"] || [] }
  redis_names = redis_peers.map { |peer| peer.dig("podSelector", "matchLabels", "app.kubernetes.io/name") }.compact.sort
  abort "Redis ingress는 app/key-rotation Pod만 허용해야 합니다." unless redis_names == %w[app key-rotation]
  redis_ports = redis_rules.flat_map { |rule| rule["ports"] || [] }.map { |port| port["port"] }.uniq
  abort "Redis ingress는 6379만 허용해야 합니다." unless redis_ports == [6379]
  grafana_prometheus_policy = policies.fetch("allow-grafana-to-prometheus")
  grafana_prometheus_rules = grafana_prometheus_policy.dig("spec", "ingress") || []
  grafana_prometheus_names = grafana_prometheus_rules
    .flat_map { |rule| rule["from"] || [] }
    .map { |peer| peer.dig("podSelector", "matchLabels", "app.kubernetes.io/name") }
    .compact
    .uniq
  abort "Prometheus ingress는 Grafana만 허용해야 합니다." unless grafana_prometheus_names == ["grafana"]
  grafana_prometheus_ports = grafana_prometheus_rules
    .flat_map { |rule| rule["ports"] || [] }
    .map { |port| port["port"] }
    .uniq
  abort "Grafana의 Prometheus 접근은 9090만 허용해야 합니다." unless grafana_prometheus_ports == [9090]
  puts "YAML 문서 #{documents.size}개 파싱 완료"
' \
    "$rendered" \
    "$REPO_ROOT/monitoring/alerts.yml" \
    "$REPO_ROOT/monitoring/dashboards/system.json" \
    "$REPO_ROOT/monitoring/dashboards/funnel.json"

ruby -ropenssl -rbase64 -e '
  nginx_path = ARGV.pop
  nginx = File.read(nginx_path)
  abort "frontend Nginx에 CSP Report-Only가 없습니다." unless nginx.include?("Content-Security-Policy-Report-Only")
  ARGV.each do |html_path|
    html = File.read(html_path)
    json_ld = html.match(%r{<script type="application/ld\+json">(.*?)</script>}m)&.[](1)
    abort "JSON-LD script를 찾을 수 없습니다: #{html_path}" unless json_ld
    json_ld_hash = "sha256-#{Base64.strict_encode64(OpenSSL::Digest::SHA256.digest(json_ld))}"
    abort "JSON-LD CSP hash가 현재 index.html과 다릅니다: #{html_path}" unless nginx.include?(json_ld_hash)
  end
  %w[
    https://js.tosspayments.com
    https://cdn.jsdelivr.net
    https://fonts.googleapis.com
    https://fonts.gstatic.com
    https://*.ingest.sentry.io
  ].each do |source|
    abort "CSP 외부 자원 허용 목록이 누락됐습니다: #{source}" unless nginx.include?(source)
  end
  abort "CSP Report-Only는 아직 중앙 report endpoint를 사용하지 않습니다." if nginx.match?(/\breport-(?:to|uri)\b/)
' \
    "$REPO_ROOT/frontend/index.html" \
    ${REQUIRE_FRONTEND_DIST:+"$REPO_ROOT/frontend/dist/index.html"} \
    "$REPO_ROOT/deploy/k3s/images/frontend-nginx.conf"

for script in "$SCRIPT_DIR"/*.sh; do
    case "$(head -n 1 "$script")" in
        *bash) bash -n "$script" ;;
        *) sh -n "$script" ;;
    esac
done

rotation_job="$tmp_dir/data-key-rotation-job.yaml"
awk '
  /^apiVersion: batch\/v1$/ { capture = 1 }
  capture && /^EOF$/ { exit }
  capture {
    gsub(/\$NAMESPACE/, "happygallery")
    gsub(/\$app_image/, "localhost/happygallery-app:test@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
    print
  }
' "$SCRIPT_DIR/rotate-data-keys.sh" > "$rotation_job"
ruby -e '
  require "yaml"
  job = YAML.load(File.read(ARGV.fetch(0)))
  abort "데이터 키 회전 Job YAML을 추출하지 못했습니다." unless job&.fetch("kind", nil) == "Job"
  template = job.dig("spec", "template")
  abort "데이터 키 회전 Job label이 올바르지 않습니다." unless template.dig("metadata", "labels", "app.kubernetes.io/name") == "key-rotation"
  container = template.dig("spec", "containers", 0)
  abort "데이터 키 회전 Job 이미지가 digest로 고정되지 않았습니다." unless container.fetch("image").match?(/@sha256:[a-f0-9]{64}\z/)
  env = container.fetch("env").to_h { |entry| [entry.fetch("name"), entry] }
  %w[SERVER_PORT MANAGEMENT_PORT].each do |name|
    abort "#{name}=0이 데이터 키 회전 Job에 없습니다." unless env.dig(name, "value") == "0"
  end
  abort "데이터 키 회전 Job은 servlet 보안 context를 유지해야 합니다." if env.key?("SPRING_MAIN_WEB_APPLICATION_TYPE")
  abort "데이터 키 회전 Job의 실행 플래그가 true가 아닙니다." unless env.dig("KEY_ROTATION_ENABLED", "value") == "true"
  required_secret_env = %w[
    FIELD_ENCRYPTION_KEY_ID ENCRYPT_KEY HMAC_KEY PREVIOUS_ENCRYPT_KEYS PREVIOUS_HMAC_KEYS
    GUEST_TOKEN_HMAC_SECRET GUEST_TOKEN_PREVIOUS_HMAC_SECRET KEY_ROTATION_SOURCE_KEY_ID
  ]
  abort "데이터 키 회전 Job의 키 Secret 참조가 누락됐습니다." unless required_secret_env.all? do |name|
    env.dig(name, "valueFrom", "secretKeyRef", "name") == "happygallery-data-key-rotation"
  end
' "$rotation_job"

grep -q 'kind: StatefulSet' "$rendered" || die "MySQL StatefulSet이 없습니다."
grep -q 'storageClassName: local-path-retain' "$rendered" || die "Retain PVC가 없습니다."
grep -q 'terminationGracePeriodSeconds: 45' "$rendered" || die "앱 종료 유예가 없습니다."
grep -q 'responseHeaderTimeout: 30s' "$rendered" || die "Traefik app 응답 헤더 timeout이 없습니다."
grep -q 'idleConnTimeout: 15s' "$rendered" || die "Traefik app upstream keep-alive timeout이 없습니다."
grep -q 'service.serverstransport: happygallery-app-upstream@kubernetescrd' "$rendered" \
    || die "app Service가 전용 Traefik ServersTransport를 사용하지 않습니다."
grep -q -- '--maxmemory 192mb --maxmemory-policy noeviction' "$rendered" \
    || die "Redis memory 상한과 noeviction 정책이 없습니다."
grep -q 'startupProbe:' "$rendered" || die "startup probe가 없습니다."
grep -q 'readinessProbe:' "$rendered" || die "readiness probe가 없습니다."
grep -q 'livenessProbe:' "$rendered" || die "liveness probe가 없습니다."
grep -q 'include: readinessState,db,redis' "$REPO_ROOT/bootstrap/src/main/resources/application-prod.yml" \
    || die "운영 readiness가 DB와 Redis를 포함하지 않습니다."
grep -q '/actuator/health/readiness' "$SCRIPT_DIR/verify.sh" \
    || die "운영 검증이 readiness endpoint를 확인하지 않습니다."
grep -q 'imagePullPolicy: Never' "$rendered" || die "로컬 이미지 import 정책이 없습니다."
grep -q 'app-management:8081' "$rendered" || die "Prometheus가 내부 관리 포트를 scrape하지 않습니다."
grep -q 'alert: PaymentConfirmReconciliationRequired' "$rendered" \
    || die "결제 confirm 수동 대사 critical 알림이 없습니다."
grep -q 'alert: PaymentReconciliationRequiredBacklog' "$rendered" \
    || die "결제 대사 DB backlog 지속 알림이 없습니다."
grep -q 'alert: NotificationLogPersistenceFailed' "$rendered" \
    || die "알림 감사 이력 저장 실패 알림이 없습니다."
grep -q 'alert: RefundActionRequiredBacklog' "$rendered" \
    || die "환불 조치 필요 backlog 알림이 없습니다."
grep -q 'alert: NotificationOutboxPendingStalled' "$rendered" \
    || die "알림 outbox 정체 알림이 없습니다."
grep -q 'alert: OperationalBacklogRefreshStalled' "$rendered" \
    || die "운영 backlog 스냅샷 정체 알림이 없습니다."
grep -q 'alertmanager:9093' "$rendered" \
    || die "Prometheus와 Alertmanager 연결이 없습니다."
grep -q 'url_file: /etc/alertmanager/secrets/webhook-url' "$rendered" \
    || die "Alertmanager webhook Secret 파일 연결이 없습니다."
grep -q 'receiver: webhook-business' "$rendered" \
    && grep -q 'category="business"' "$rendered" \
    && grep -q 'repeat_interval: 30m' "$rendered" \
    || die "업무 backlog 경보의 30분 재알림 경로가 없습니다."
grep -q 'GOOGLE_OAUTH_REDIRECT_URI: https://gallery.example.com/api/v1/auth/social/callback/google' "$rendered" \
    || die "Google OAuth callback이 공개 host와 일치하지 않습니다."
grep -q 'NAVER_OAUTH_REDIRECT_URI: https://gallery.example.com/api/v1/auth/social/callback/naver' "$rendered" \
    || die "Naver OAuth callback이 공개 host와 일치하지 않습니다."

if grep -Eq 'type: (NodePort|LoadBalancer)|hostPort:' "$rendered"; then
    die "Ingress 외 직접 공개 포트가 manifest에 있습니다."
fi
if grep -Eq 'image: [^[:space:]]*:latest([[:space:]]|$)' "$rendered"; then
    die "latest 이미지 태그를 사용할 수 없습니다."
fi

grep -q 'wait_for_no_pods.*app.kubernetes.io/name=app' "$SCRIPT_DIR/restore-mysql.sh" \
    || die "복원 스크립트가 실제 app Pod 종료를 기다리지 않습니다."
grep -q 'set image deployment/app' "$SCRIPT_DIR/rollback.sh" \
    || die "rollback이 app digest 이미지를 직접 지정하지 않습니다."
if grep -Eq 'apply .*manifests|rollout status statefulset/mysql|rollout status deployment/redis' "$SCRIPT_DIR/rollback.sh"; then
    die "rollback이 stateful 또는 전체 release manifest를 재적용할 수 있습니다."
fi
grep -q 'rotate-mysql-credentials.sh' "$SCRIPT_DIR/create-secrets.sh" \
    || die "기존 PVC의 MySQL Secret 단독 교체 방지가 없습니다."
grep -q 'rotate-redis-credentials.sh' "$SCRIPT_DIR/create-secrets.sh" \
    || die "Redis Secret 단독 교체 방지가 없습니다."
grep -q 'rotate-data-keys.sh/finalize-data-key-rotation.sh' "$SCRIPT_DIR/create-secrets.sh" \
    || die "데이터 키/키링의 일반 Secret 교체 방지가 없습니다."
grep -q 'activate-restored-release.sh' "$SCRIPT_DIR/restore-recovery-backup.sh" \
    || die "데이터 복원 후 guarded 호환 digest 활성화 절차 안내가 없습니다."
grep -q 'k3s_ctr images export' "$SCRIPT_DIR/backup-mysql.sh" \
    || die "off-device 백업에 호환 app/frontend 이미지 archive가 없습니다."
grep -q -- '--references "$release_manifest"' "$SCRIPT_DIR/backup-mysql.sh" \
    || die "off-device 백업이 release manifest에서 runtime 이미지를 추출하지 않습니다."
grep -q -- '--inventory "$release_tmp/manifests.yaml" "$runtime_metadata"' \
    "$SCRIPT_DIR/backup-mysql.sh" \
    || die "off-device 백업이 parser의 runtime image inventory를 사용하지 않습니다."
grep -q -- '--inventory "$manifest" "$runtime_metadata"' \
    "$SCRIPT_DIR/prepare-restored-release-images.sh" \
    || die "복원 준비가 보존 manifest와 metadata를 하나의 runtime image inventory로 검증하지 않습니다."
while IFS="$tab" read -r runtime_key runtime_image unexpected; do
    for consumer in \
        "$SCRIPT_DIR/backup-mysql.sh" \
        "$SCRIPT_DIR/prepare-restored-release-images.sh"; do
        if grep -q "${runtime_key}_IMAGE" "$consumer"; then
            die "runtime workload 목록은 manifest parser만 소유해야 합니다: $consumer"
        fi
    done
done < "$runtime_references"
grep -q 'FLYWAY_SCHEMA_VERSION=' "$SCRIPT_DIR/backup-mysql.sh" \
    || die "복구 메타데이터에 Flyway version이 없습니다."
grep -q 'FIELD_ENCRYPTION_KEY_ID=' "$SCRIPT_DIR/backup-mysql.sh" \
    || die "복구 메타데이터에 암호화 키 ID가 없습니다."
grep -q 'ensure_media_pvc' "$SCRIPT_DIR/backup-mysql.sh" \
    || die "기존 클러스터의 첫 미디어 백업을 위한 PVC 사전 생성이 없습니다."
grep -q 'VERIFIED_RECOVERY_BUNDLE' "$SCRIPT_DIR/rollout.sh" \
    || die "rollout이 완성된 복구 묶음 marker를 요구하지 않습니다."
grep -q 'verify_recovery_bundle_files.*recovery_bundle' "$SCRIPT_DIR/rollout.sh" \
    || die "rollout이 DB·미디어·release 복구 묶음 전체를 검증하지 않습니다."
if grep -q 'VERIFIED_BACKUP_FILE' "$SCRIPT_DIR/rollout.sh"; then
    die "rollout에 DB 단일 파일만 검증하는 이전 계약이 남아 있습니다."
fi
grep -q 'k3s_ctr images import' "$SCRIPT_DIR/prepare-restored-release-images.sh" \
    || die "복원 release가 외부 이미지 archive를 containerd에 가져오지 않습니다."
grep -q 'prepare-restored-release-images.sh.*release_dir' "$SCRIPT_DIR/restore-mysql.sh" \
    || die "파괴적 DB 복원 전에 호환 release 이미지 선검증이 없습니다."
grep -q 'restored_flyway_version' "$SCRIPT_DIR/restore-recovery-backup.sh" \
    || die "복구 진입점이 restored Flyway version을 확인하지 않습니다."
grep -q 'backup_created_at=.*BACKUP_CREATED_AT' "$SCRIPT_DIR/restore-recovery-backup.sh" \
    && grep -q 'recovery_metadata_sha256=.*sha256_file.*recovery_metadata' \
        "$SCRIPT_DIR/restore-recovery-backup.sh" \
    && grep -q 'reconciliation_token=.*backup_created_at.*recovery_metadata_sha256' \
        "$SCRIPT_DIR/restore-recovery-backup.sh" \
    && grep -q 'pending_marker=.*reconciliation_token.*pending' \
        "$SCRIPT_DIR/restore-recovery-backup.sh" \
    || die "복구 진입점이 backup timestamp와 recovery metadata checksum으로 대사 토큰을 발급하지 않습니다."
if grep -q 'activate-restored-release.sh.*release_dir' "$SCRIPT_DIR/restore-recovery-backup.sh"; then
    die "데이터 복원 진입점이 운영 대사 전에 app을 자동 활성화합니다."
fi
if grep -Eq 'CONFIRM_RESTORED_(PAYMENT|NOTIFICATION|PRIVACY_REQUEST)_RECONCILIATION:-}" = "\$IMAGE_TAG"' \
    "$SCRIPT_DIR/activate-restored-release.sh"; then
    die "복원 대사 확인이 backup이 아닌 IMAGE_TAG에만 결합되어 있습니다."
fi
grep -q 'runtime_key_id.*expected_key_id' "$SCRIPT_DIR/restore-recovery-backup.sh" \
    || die "복구 진입점이 runtime 암호화 키 ID를 확인하지 않습니다."
grep -q 'runtime_rotation_phase.*expected_rotation_phase' "$SCRIPT_DIR/restore-recovery-backup.sh" \
    || die "복구 진입점이 키 회전 단계를 확인하지 않습니다."
grep -q 'verify_checksum.*images_archive' "$SCRIPT_DIR/restore-recovery-backup.sh" \
    || die "복구 진입점이 이미지 archive 무결성을 선검증하지 않습니다."
grep -q 'OnFailure=happygallery-backup-failure@%n.service' \
    "$DEPLOY_DIR/systemd/happygallery-backup.service.example" \
    || die "백업 실패 systemd 알림 연결이 없습니다."
grep -q 'backup.last-success' "$DEPLOY_DIR/systemd/happygallery-backup.service.example" \
    || die "백업 성공 heartbeat 파일이 없습니다."
grep -q 'TimeoutStartSec=30m' "$DEPLOY_DIR/systemd/happygallery-backup.service.example" \
    && grep -q 'TimeoutStopSec=10m' "$DEPLOY_DIR/systemd/happygallery-backup.service.example" \
    || die "백업 실행 제한과 app 원복 종료 유예가 없습니다."
grep -q 'manage-backup-alert-silence.sh start' \
    "$DEPLOY_DIR/systemd/happygallery-backup.service.example" \
    && grep -q 'manage-backup-alert-silence.sh stop' \
        "$DEPLOY_DIR/systemd/happygallery-backup.service.example" \
    && grep -q 'value: "AppDown"' "$SCRIPT_DIR/manage-backup-alert-silence.sh" \
    && grep -q 'now + 45 \* 60' "$SCRIPT_DIR/manage-backup-alert-silence.sh" \
    || die "계획 백업 AppDown silence의 생성·해제·만료 안전장치가 없습니다."
grep -q 'OnFailure=happygallery-backup-failure@%n.service' \
    "$DEPLOY_DIR/systemd/happygallery-backup-watchdog.service.example" \
    || die "백업 heartbeat watchdog 실패 알림 연결이 없습니다."
grep -q 'backup.last-success 25200' \
    "$DEPLOY_DIR/systemd/happygallery-backup-watchdog.service.example" \
    || die "백업 heartbeat watchdog의 7시간 기준이 없습니다."

heartbeat_file="$tmp_dir/backup.last-success"
touch "$heartbeat_file"
"$SCRIPT_DIR/check-backup-heartbeat.sh" "$heartbeat_file" 60 >/dev/null
touch -t 200001010000 "$heartbeat_file"
if "$SCRIPT_DIR/check-backup-heartbeat.sh" "$heartbeat_file" 60 >/dev/null 2>&1; then
    die "백업 heartbeat watchdog이 오래된 파일을 정상으로 처리했습니다."
fi
grep -q 'APP_IMAGE_DIGEST=' "$SCRIPT_DIR/build-import-images.sh" \
    || die "이미지 build/import 결과에 digest가 없습니다."
if grep -Eq '(MYSQL_ROOT_PASSWORD|DB_PASSWORD|ENCRYPT_KEY|HMAC_KEY|PREVIOUS_ENCRYPT_KEYS|PREVIOUS_HMAC_KEYS|GUEST_TOKEN_HMAC_SECRET|GUEST_TOKEN_PREVIOUS_HMAC_SECRET|TOSS_SECRET_KEY): [^[:space:]]+' "$rendered"; then
    die "평문 secret으로 의심되는 값이 manifest에 있습니다."
fi

ruby - "$SCRIPT_DIR" <<'RUBY'
  script_dir = ARGV.fetch(0)
  create_secrets = File.read(File.join(script_dir, "create-secrets.sh"))
  guarded_data_keys = %w[
    FIELD_ENCRYPTION_KEY_ID ENCRYPT_KEY HMAC_KEY PREVIOUS_ENCRYPT_KEYS PREVIOUS_HMAC_KEYS
    GUEST_TOKEN_HMAC_SECRET GUEST_TOKEN_PREVIOUS_HMAC_SECRET
  ]
  guarded_data_keys.each do |key|
    abort "#{key}의 일반 Secret 교체 차단이 없습니다." unless create_secrets.include?("guard_data_key_change #{key}")
  end
  data_key_guard = /guard_data_key_change\(\).*?current_encoded=.*?get secret happygallery-app.*?desired_encoded=.*?\[ "\$current_encoded" = "\$desired_encoded" \]/m
  abort "데이터 결합 키의 기존 값 비교가 없습니다." unless create_secrets.match?(data_key_guard)
  abort "데이터 결합 키 변경이 전용 절차로 연결되지 않습니다." unless create_secrets.include?("rotate-data-keys.sh/finalize-data-key-rotation.sh")
  legacy_id_guard = /key.*FIELD_ENCRYPTION_KEY_ID.*?current_encoded.*?desired_value.*?v1/m
  abort "기존 Secret의 누락된 key ID를 v1 기본값으로 보정할 수 없습니다." unless create_secrets.match?(legacy_id_guard)
  abort "기존 MySQL PVC에서 app Secret 유실을 차단하지 않습니다." unless create_secrets.match?(/get pvc data-mysql-0.*?get secret happygallery-app.*?분리 보관한 기존 Secret/m)
  redis_guard = /get secret happygallery-redis.*?current_redis_encoded=.*?desired_redis_encoded=.*?\[ "\$current_redis_encoded" = "\$desired_redis_encoded" \].*?rotate-redis-credentials\.sh/m
  abort "Redis 비밀번호의 일반 Secret 교체 차단이 없습니다." unless create_secrets.match?(redis_guard)
  abort "기존 Redis deployment에서 Secret 유실을 차단하지 않습니다." unless create_secrets.match?(/get deployment redis.*?rotate-redis-credentials\.sh로 app을 중지/m)

  redis_rotation = File.read(File.join(script_dir, "rotate-redis-credentials.sh"))
  redis_flow = /Redis 비밀번호 불일치를 막기 위해.*?scale deployment\/app --replicas=0.*?wait_for_no_pods.*?patch secret happygallery-redis.*?rollout restart deployment\/redis.*?scale deployment\/app --replicas=1/m
  abort "Redis 회전의 app drain/Secret/Redis/app 순서가 깨졌습니다." unless redis_rotation.match?(redis_flow)
  abort "유실된 Redis Secret의 안전한 재생성 경로가 없습니다." unless redis_rotation.match?(/Redis Secret이 없어 app을 중지.*?create secret generic happygallery-redis/m)
  abort "Redis 회전 실패 시 app drain 보장이 없습니다." unless redis_rotation.match?(/on_rotation_error\(\).*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m)

  data_rotation = File.read(File.join(script_dir, "rotate-data-keys.sh"))
  data_rotation_flow = /키 회전 중 쓰기를 막기 위해.*?scale deployment\/app --replicas=0.*?wait_for_no_pods.*?fresh off-device.*?backup-mysql\.sh.*?verify_checksum.*?kind: Job.*?SERVER_PORT.*?MANAGEMENT_PORT.*?KEY_ROTATION_ENABLED.*?wait_for_rotation_job.*?runtime Secret을 새 active.*?patch secret happygallery-app.*?FLUSHALL.*?scale deployment\/app --replicas=1/m
  abort "데이터 키 회전의 drain/backup/Job/Secret/Redis/app 순서가 깨졌습니다." unless data_rotation.match?(data_rotation_flow)
  abort "데이터 키 회전 Job이 현재 app digest를 재사용하지 않습니다." unless data_rotation.match?(/app_image=.*?containers.*?image: \$app_image/m)
  abort "데이터 키 회전 Job이 외부 Service와 분리된 key-rotation label을 쓰지 않습니다." unless data_rotation.match?(/app\.kubernetes\.io\/name: key-rotation/)
  abort "데이터 키 회전 실패 시 app drain 보장이 없습니다." unless data_rotation.match?(/on_rotation_error\(\).*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m)
  abort "데이터 키 회전이 guest previous 보존기한을 기록하지 않습니다." unless data_rotation.include?("guest-previous-valid-until-epoch")
  abort "데이터 키 회전이 guest 결제 인증 증거 경계를 기록하지 않습니다." unless data_rotation.include?("guest-proof-previous-issued-before-epoch")
  abort "데이터 키 회전 시작 상태와 원래 replica 기록이 없습니다." unless data_rotation.match?(/key-rotation-phase.*?started.*?key-rotation-original-replicas/m)
  abort "runtime app에서 key rotation 실행을 차단하지 않습니다." unless data_rotation.match?(/current_rotation_enabled.*?''\|false/m)

  data_finalization = File.read(File.join(script_dir, "finalize-data-key-rotation.sh"))
  abort "previous 키 finalize가 소셜 provider ID 백필을 검사하지 않습니다." unless data_finalization.include?("provider_id_enc IS NULL")
  abort "previous 키 finalize가 비회원 결제 휴대폰 HMAC 키 전환을 검사하지 않습니다." unless data_finalization.include?("owner_phone_hmac_key_id")
  abort "previous 키 finalize가 관리자 TOTP 암호문 키 전환을 검사하지 않습니다." unless data_finalization.include?("pending_admin_totp_secrets") && data_finalization.include?("totp_secret_enc")
  abort "previous guest 키 finalize가 회전 전 비회원 결제 인증 증거를 검사하지 않습니다." unless data_finalization.include?("pending_guest_payment_proofs")
  abort "guest 결제 인증 증거 회전 경계 annotation이 없습니다." unless data_finalization.include?("guest-proof-previous-issued-before-epoch")
  finalization_flow = /guest_previous_valid_until.*?now_epoch.*?scale deployment\/app --replicas=0.*?wait_for_no_pods.*?pending=\$\(pending_social_accounts\).*?PREVIOUS_ENCRYPT_KEYS.*?PREVIOUS_HMAC_KEYS.*?GUEST_TOKEN_PREVIOUS_HMAC_SECRET.*?scale deployment\/app --replicas=1/m
  abort "previous 키 finalize의 guest 유예/social 백필/drain/Secret/app 순서가 깨졌습니다." unless data_finalization.match?(finalization_flow)
  abort "previous 키 finalize 실패 시 app drain 보장이 없습니다." unless data_finalization.match?(/on_finalization_error\(\).*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m)
  abort "previous 키 finalize 재개 상태가 없습니다." unless data_finalization.match?(/key-rotation-phase.*?finalizing.*?key-finalization-original-replicas/m)

  restored_release = File.read(File.join(script_dir, "activate-restored-release.sh"))
  restore_entry = File.read(File.join(script_dir, "restore-recovery-backup.sh"))
  token_issue = /BACKUP_CREATED_AT.*?sha256_file "\$recovery_metadata".*?reconciliation_token=.*?pending_marker=.*?RECOVERY_METADATA_SHA256=.*?mv "\$pending_marker_tmp" "\$pending_marker"/m
  abort "복원 성공 후 timestamp/checksum 대사 토큰 marker 발급 순서가 없습니다." unless restore_entry.match?(token_issue)
  abort "미완료 복원 대사 marker가 있을 때 새 복원을 막지 않습니다." unless restore_entry.match?(/existing_unfinished_marker=.*?name '\*\.pending'.*?name '\*\.activating'.*?die/m)
  restore_lock_flow = /restore_lock_dir=.*?cleanup_restore\(\).*?restore_lock_held.*?rmdir "\$restore_lock_dir".*?trap cleanup_restore EXIT.*?mkdir "\$restore_lock_dir".*?restore_lock_held=true.*?existing_unfinished_marker=.*?restore-mysql\.sh.*?find \/media -mindepth 1 -depth -delete.*?mv "\$pending_marker_tmp" "\$pending_marker"/m
  abort "DB·미디어 교체 전 원자적 복원 실행 락 선점과 종료 시 해제가 없습니다." unless restore_entry.match?(restore_lock_flow)
  abort "락을 얻지 못한 경쟁 실행이 기존 락을 지울 수 있습니다." unless restore_entry.match?(/restore_lock_held=false.*?if \[ "\$restore_lock_held" = true \].*?rmdir "\$restore_lock_dir"/m)
  reconciliation_gate = /RESTORE_RECONCILIATION_TOKEN.*?CONFIRM_RESTORED_PAYMENT_RECONCILIATION.*?reconciliation_token.*?CONFIRM_RESTORED_NOTIFICATION_RECONCILIATION.*?reconciliation_token.*?CONFIRM_RESTORED_PRIVACY_REQUEST_RECONCILIATION.*?reconciliation_token.*?require_private_file "\$pending_marker".*?RECOVERY_METADATA_SHA256.*?marker_created_at-\$marker_metadata_sha256.*?IMAGE_TAG.*?RELEASE_DIR/m
  abort "복원 release 활성화가 일치하는 timestamp/checksum marker와 PG·알림·개인정보 요청 대사 확인을 요구하지 않습니다." unless restored_release.match?(reconciliation_gate)
  one_time_token_flow = /mv "\$pending_marker" "\$activating_marker".*?scale deployment\/app --replicas=1.*?rollout status deployment\/app.*?mv "\$activating_marker" "\$consumed_marker"/m
  abort "복원 대사 토큰의 pending/activating/consumed 일회 소비 순서가 없습니다." unless restored_release.match?(one_time_token_flow)
  activation_exit_cleanup = /activation_succeeded=false.*?activation_cleanup_done=false.*?on_activation_exit\(\).*?trap - EXIT HUP INT TERM.*?activation_cleanup_done.*?activation_started.*?activation_succeeded.*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m
  abort "복원 release의 명시적 오류와 일반 종료를 한 번만 처리하는 EXIT fail-closed 정리가 없습니다." unless restored_release.match?(activation_exit_cleanup)
  signal_cleanup = /trap on_activation_exit EXIT.*?trap 'exit 129' HUP.*?trap 'exit 130' INT.*?trap 'exit 143' TERM.*?activation_started=true.*?mv "\$pending_marker" "\$activating_marker"/m
  abort "복원 release 활성화 시작 전에 HUP/INT/TERM을 EXIT fail-closed 정리로 연결하지 않습니다." unless restored_release.match?(signal_cleanup)
  marker_recovery = /app_drained=true.*?if \[ "\$app_drained" = true \].*?mv "\$activating_marker" "\$pending_marker".*?mv "\$consumed_marker" "\$pending_marker".*?marker_recovered/m
  abort "활성화 실패 후 app drain 성공 때 대사 토큰을 재시도 가능한 pending 상태로 복구하지 않습니다." unless restored_release.match?(marker_recovery)
  abort "복원 release 성공 marker 이전 종료가 성공으로 처리될 수 있습니다." unless restored_release.match?(/mv "\$activating_marker" "\$consumed_marker".*?activation_succeeded=true/m)
  abort "ERR와 EXIT가 복원 release 실패 정리를 중복 실행할 수 있습니다." if restored_release.match?(/trap [^\n]+ ERR/)
  activation_flow = /app이 중지된 상태에서.*?set image deployment\/app.*?configured_app_ref=.*?대사 확인.*?scale deployment\/app --replicas=1/m
  abort "복원 release가 app scale-up 전에 호환 digest를 확정하지 않습니다." unless restored_release.match?(activation_flow)

  image_preflight = File.read(File.join(script_dir, "prepare-restored-release-images.sh"))
  required_images = /runtime_inventory=.*?runtime-images-from-manifest\.rb.*?--inventory.*?all_required_images_match\(\).*?containerd_has_image "\$app_ref".*?containerd_has_image "\$frontend_ref".*?while IFS=.*?read -r runtime_key runtime_image expected_digest unexpected.*?k3s_ctr images import.*?all_required_images_match/m
  abort "DB 복원 전 app/frontend/runtime 이미지 import와 digest 재검증이 없습니다." unless image_preflight.match?(required_images)

  common = File.read(File.join(script_dir, "common.sh"))
  bundle_validation = /verify_recovery_bundle_files\(\).*?require_private_file.*?verify_checksum.*?DATABASE_BACKUP.*?MEDIA_BACKUP.*?RELEASE_DIR.*?metadata\.env.*?manifests\.yaml.*?runtime-images\.env.*?images\.tar.*?verify_checksum/m
  abort "복구 묶음 marker와 DB·미디어·release sidecar 전체 검증이 없습니다." unless common.match?(bundle_validation)

  backup = File.read(File.join(script_dir, "backup-mysql.sh"))
  runtime_archive = /--references "\$release_manifest".*?while IFS=.*?read -r runtime_key runtime_image unexpected.*?containerd_image_digest.*?for runtime_index in.*?--inventory "\$release_tmp\/manifests\.yaml" "\$runtime_metadata".*?archive_images=.*?while IFS=.*?read -r runtime_key runtime_image runtime_digest unexpected.*?k3s_ctr images export "\$images_archive" "\$\{archive_images\[@\]\}"/m
  abort "백업이 parser inventory를 runtime metadata와 archive export에 일관되게 사용하지 않습니다." unless backup.match?(runtime_archive)
  backup_exclusion = /original_app_replicas=.*?scale deployment\/app --replicas=0.*?wait_for_no_pods.*?mysqldump.*?start_media_helper.*?restore_app/m
  abort "백업의 app 쓰기 중단과 원래 replica 복구 순서가 깨졌습니다." unless backup.match?(backup_exclusion)
  abort "백업 실패 시 원래 app replica 복구가 없습니다." unless backup.match?(/cleanup_partial_backup\(\).*?restore_app/m)
  bundle_publish = /mv "\$tmp" "\$backup".*?mv "\$tmp_checksum" "\$backup\.sha256".*?mv "\$media_tmp" "\$media_backup".*?mv "\$media_tmp_checksum" "\$media_backup\.sha256".*?mv "\$recovery_metadata_tmp_checksum" "\$recovery_metadata\.sha256".*?mv "\$recovery_metadata_tmp" "\$recovery_metadata"/m
  abort "recovery.env가 모든 archive와 sidecar 뒤에 commit marker로 게시되지 않습니다." unless backup.match?(bundle_publish)

  backup_timer = File.read(File.join(script_dir, "..", "systemd", "happygallery-backup.timer.example"))
  abort "백업 timer의 네 실행 시각에 Asia/Seoul이 명시되지 않았습니다." unless
    backup_timer.scan(/^OnCalendar=.*Asia\/Seoul$/).size == 4
  watchdog_timer = File.read(File.join(script_dir, "..", "systemd", "happygallery-backup-watchdog.timer.example"))
  abort "백업 watchdog은 독립된 15분 timer여야 합니다." unless
    watchdog_timer.include?("OnUnitActiveSec=15m") &&
    watchdog_timer.include?("Unit=happygallery-backup-watchdog.service") &&
    watchdog_timer.include?("Persistent=true")
RUBY

bash "$SCRIPT_DIR/tests/rotate-mysql-credentials-test.sh"

info "k3s manifest와 운영 스크립트 검증 완료"
