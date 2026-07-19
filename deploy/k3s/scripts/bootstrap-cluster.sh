#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -le 1 ] || die "사용법: $0 [cert-manager.yaml]"
cert_manager_manifest=${1:-}

kube get nodes >/dev/null
node_count=$(kube get nodes --no-headers | awk 'END { print NR }')
[ "$node_count" -eq 1 ] || die "이 구성은 단일 노드 전용입니다. 현재 노드 수: $node_count"

if command -v k3s >/dev/null 2>&1; then
    if [ "$(id -u)" -eq 0 ]; then
        k3s secrets-encrypt status | grep -qi enabled \
            || die "k3s secrets encryption at rest를 활성화하세요."
    else
        info "일반 사용자 실행이라 k3s secrets-encrypt 상태 확인을 건너뜁니다. root로 별도 확인하세요."
    fi
fi

info "Traefik이 외부 X-Forwarded-* 값을 무조건 신뢰하지 않도록 설정합니다."
kube apply -f "$DEPLOY_DIR/cluster/traefik-config.yaml"
kube -n kube-system rollout status deployment/traefik --timeout=5m
traefik_pods=$(kube -n kube-system get pods \
    -l app.kubernetes.io/name=traefik --no-headers 2>/dev/null | awk 'END { print NR }')
[ "$traefik_pods" -ge 1 ] \
    || die "NetworkPolicy가 요구하는 Traefik label(app.kubernetes.io/name=traefik)을 찾을 수 없습니다."

if kube get crd certificates.cert-manager.io >/dev/null 2>&1; then
    info "cert-manager CRD가 이미 설치되어 있습니다."
else
    [ -n "$cert_manager_manifest" ] \
        || die "검증해 둔 cert-manager v1.20.2 manifest 경로가 필요합니다."
    [ -f "$cert_manager_manifest" ] || die "cert-manager manifest를 찾을 수 없습니다: $cert_manager_manifest"
    kube apply -f "$cert_manager_manifest"
fi

kube -n cert-manager rollout status deployment/cert-manager --timeout=5m
kube -n cert-manager rollout status deployment/cert-manager-webhook --timeout=5m
kube get storageclass local-path >/dev/null \
    || die "k3s local-path provisioner StorageClass를 찾을 수 없습니다."

info "클러스터 공통 구성 완료"
