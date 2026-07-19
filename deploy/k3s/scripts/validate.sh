#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

require_command ruby
require_command grep

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

ruby -e '
  require "yaml"
  documents = YAML.load_stream(File.read(ARGV.fetch(0)))
  abort "빈 Kubernetes 문서가 있습니다." if documents.any?(&:nil?)
  abort "apiVersion/kind가 없는 문서가 있습니다." unless documents.all? { |d| d["apiVersion"] && d["kind"] }
  abort "평문 Secret manifest를 만들 수 없습니다." if documents.any? { |d| d["kind"] == "Secret" }
  ingresses = documents.select { |d| d["kind"] == "Ingress" }
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
  singleton_deployments = documents.select do |d|
    d["kind"] == "Deployment" && %w[redis alertmanager].include?(d.dig("metadata", "name"))
  end
  abort "Redis/Alertmanager 단일 인스턴스는 Recreate 전략이어야 합니다." unless singleton_deployments.size == 2 && singleton_deployments.all? do |d|
    d.dig("spec", "replicas") == 1 && d.dig("spec", "strategy", "type") == "Recreate"
  end
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
  puts "YAML 문서 #{documents.size}개 파싱 완료"
' "$rendered"

for script in "$SCRIPT_DIR"/*.sh; do
    case "$(head -n 1 "$script")" in
        *bash) bash -n "$script" ;;
        *) sh -n "$script" ;;
    esac
done

grep -q 'kind: StatefulSet' "$rendered" || die "MySQL StatefulSet이 없습니다."
grep -q 'storageClassName: local-path-retain' "$rendered" || die "Retain PVC가 없습니다."
grep -q 'terminationGracePeriodSeconds: 45' "$rendered" || die "앱 종료 유예가 없습니다."
grep -q 'startupProbe:' "$rendered" || die "startup probe가 없습니다."
grep -q 'readinessProbe:' "$rendered" || die "readiness probe가 없습니다."
grep -q 'livenessProbe:' "$rendered" || die "liveness probe가 없습니다."
grep -q 'imagePullPolicy: Never' "$rendered" || die "로컬 이미지 import 정책이 없습니다."
grep -q 'app-management:8081' "$rendered" || die "Prometheus가 내부 관리 포트를 scrape하지 않습니다."
grep -q 'alert: PaymentConfirmReconciliationRequired' "$rendered" \
    || die "결제 confirm 수동 대사 critical 알림이 없습니다."
grep -q 'alertmanager:9093' "$rendered" \
    || die "Prometheus와 Alertmanager 연결이 없습니다."
grep -q 'url_file: /etc/alertmanager/secrets/webhook-url' "$rendered" \
    || die "Alertmanager webhook Secret 파일 연결이 없습니다."
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
grep -q 'activate-restored-release.sh' "$SCRIPT_DIR/restore-mysql.sh" \
    || die "복원 후 호환 digest 활성화 절차가 연결되지 않았습니다."
grep -q 'APP_IMAGE_DIGEST=' "$SCRIPT_DIR/build-import-images.sh" \
    || die "이미지 build/import 결과에 digest가 없습니다."
if grep -Eq '(MYSQL_ROOT_PASSWORD|DB_PASSWORD|ENCRYPT_KEY|HMAC_KEY|TOSS_SECRET_KEY): [^[:space:]]+' "$rendered"; then
    die "평문 secret으로 의심되는 값이 manifest에 있습니다."
fi

ruby - "$SCRIPT_DIR" <<'RUBY'
  script_dir = ARGV.fetch(0)
  create_secrets = File.read(File.join(script_dir, "create-secrets.sh"))
  data_key_guard = /get secret happygallery-app.*?"ENCRYPT_KEY\|.*?"HMAC_KEY\|.*?"GUEST_TOKEN_HMAC_SECRET\|.*?current_encoded=.*?desired_encoded=.*?\[ "\$current_encoded" = "\$desired_encoded" \].*?별도 키 회전 절차가 구현될 때까지/m
  abort "데이터 결합 키의 기존 값 비교·차단이 없습니다." unless create_secrets.match?(data_key_guard)
  abort "기존 MySQL PVC에서 app Secret 유실을 차단하지 않습니다." unless create_secrets.match?(/get pvc data-mysql-0.*?get secret happygallery-app.*?분리 보관한 기존 Secret/m)
  redis_guard = /get secret happygallery-redis.*?current_redis_encoded=.*?desired_redis_encoded=.*?\[ "\$current_redis_encoded" = "\$desired_redis_encoded" \].*?rotate-redis-credentials\.sh/m
  abort "Redis 비밀번호의 일반 Secret 교체 차단이 없습니다." unless create_secrets.match?(redis_guard)
  abort "기존 Redis deployment에서 Secret 유실을 차단하지 않습니다." unless create_secrets.match?(/get deployment redis.*?rotate-redis-credentials\.sh로 app을 중지/m)

  redis_rotation = File.read(File.join(script_dir, "rotate-redis-credentials.sh"))
  redis_flow = /Redis 비밀번호 불일치를 막기 위해.*?scale deployment\/app --replicas=0.*?wait_for_no_pods.*?patch secret happygallery-redis.*?rollout restart deployment\/redis.*?scale deployment\/app --replicas=1/m
  abort "Redis 회전의 app drain/Secret/Redis/app 순서가 깨졌습니다." unless redis_rotation.match?(redis_flow)
  abort "유실된 Redis Secret의 안전한 재생성 경로가 없습니다." unless redis_rotation.match?(/Redis Secret이 없어 app을 중지.*?create secret generic happygallery-redis/m)
  abort "Redis 회전 실패 시 app drain 보장이 없습니다." unless redis_rotation.match?(/on_rotation_error\(\).*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m)

  mysql_rotation = File.read(File.join(script_dir, "rotate-mysql-credentials.sh"))
  abort "MySQL 회전 실패 시 app drain 보장이 없습니다." unless mysql_rotation.match?(/on_rotation_error\(\).*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m)

  restored_release = File.read(File.join(script_dir, "activate-restored-release.sh"))
  activation_flow = /app이 중지된 상태에서.*?set image deployment\/app.*?configured_app_ref=.*?scale deployment\/app --replicas=1/m
  abort "복원 release가 app scale-up 전에 호환 digest를 확정하지 않습니다." unless restored_release.match?(activation_flow)
  abort "복원 release 활성화 실패 시 app drain 보장이 없습니다." unless restored_release.match?(/on_activation_error\(\).*?scale deployment\/app --replicas=0.*?wait_for_no_pods/m)
RUBY

info "k3s manifest와 운영 스크립트 정적 검증 완료"
