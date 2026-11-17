#!/usr/bin/env bash

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"
. "$SCRIPT_DIR/rolling-support.sh"

[ "$#" -ge 1 ] && [ "$#" -le 2 ] || die "사용법: $0 <release.env> [apply|prepare|resume|complete|failed]"
release_env=$1
phase=${2:-apply}
case "$phase" in apply|prepare|resume|complete|failed) ;; *) die "알 수 없는 배포 단계입니다: $phase" ;; esac
validate_env_file "$release_env"

state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
if [ "$phase" = complete ] || [ "$phase" = failed ] || [ "$phase" = resume ]; then
    [ -f "$state_root/gitops-active" ] || die "진행 중인 GitOps release가 없습니다."
    release_dir=$(cat "$state_root/gitops-active")
    # 호스트 실행기가 생성한 600 권한의 상태만 읽는다. Git이나 요청 본문을 source하지 않는다.
    . "$release_dir/gitops-state.sh"
    [ "$IMAGE_TAG" = "$(require_env_value IMAGE_TAG "$release_env")" ] || die "진행 중인 release와 요청이 다릅니다."
    if [ "$phase" = resume ]; then
        [ -n "$(find "$release_dir" -prune -mtime -2 -print)" ] || die "준비한 release가 48시간보다 오래됐습니다. 운영자 확인이 필요합니다."
        start_media_helper "$APP_IMAGE@$APP_IMAGE_DIGEST"
        kube -n "$NAMESPACE" exec "$MEDIA_HELPER_POD" -- sh -ec \
            'mkdir -p /media/.deployment-in-progress; printf "%s\n" "$1" > /media/.deployment-in-progress/owner' sh "$rolling_guard_owner"
        exit 0
    fi
    rolling_apply_started=true
    if [ "$phase" = failed ]; then
        rolling_cleanup
        info "동기화 실패 상태를 보존했습니다. 같은 release를 complete로 재시도하세요: $release_dir"
        exit 0
    fi
    trap rolling_cleanup EXIT
    start_media_helper "$APP_IMAGE@$APP_IMAGE_DIGEST"
    rolling_finish
    rm "$state_root/gitops-active"
    exit 0
fi
[ ! -f "$state_root/gitops-active" ] || die "이전 GitOps release를 먼저 완료하세요."

PUBLIC_HOST=$(require_env_value PUBLIC_HOST "$release_env")
ACME_EMAIL=$(require_env_value ACME_EMAIL "$release_env")
APP_IMAGE=$(require_env_value APP_IMAGE "$release_env")
FRONTEND_IMAGE=$(require_env_value FRONTEND_IMAGE "$release_env")
APP_IMAGE_DIGEST=$(require_env_value APP_IMAGE_DIGEST "$release_env")
FRONTEND_IMAGE_DIGEST=$(require_env_value FRONTEND_IMAGE_DIGEST "$release_env")
IMAGE_TAG=$(require_env_value IMAGE_TAG "$release_env")
GOOGLE_OAUTH_REDIRECT_URI=$(require_env_value GOOGLE_OAUTH_REDIRECT_URI "$release_env")
NAVER_OAUTH_REDIRECT_URI=$(require_env_value NAVER_OAUTH_REDIRECT_URI "$release_env")
KAKAO_OAUTH_REDIRECT_URI=$(require_env_value KAKAO_OAUTH_REDIRECT_URI "$release_env")
IMAGE_PULL_POLICY=Never
case "$APP_IMAGE" in
    ghcr.io/ljkhyeong/happygallery-app:*) IMAGE_PULL_POLICY=IfNotPresent ;;
esac
export PUBLIC_HOST ACME_EMAIL APP_IMAGE FRONTEND_IMAGE APP_IMAGE_DIGEST FRONTEND_IMAGE_DIGEST IMAGE_TAG IMAGE_PULL_POLICY

[ "$GOOGLE_OAUTH_REDIRECT_URI" = "https://$PUBLIC_HOST/api/v1/auth/social/callback/google" ] \
    || die "GOOGLE_OAUTH_REDIRECT_URI가 공개 host의 exact callback과 다릅니다."
[ "$NAVER_OAUTH_REDIRECT_URI" = "https://$PUBLIC_HOST/api/v1/auth/social/callback/naver" ] \
    || die "NAVER_OAUTH_REDIRECT_URI가 공개 host의 exact callback과 다릅니다."
[ "$KAKAO_OAUTH_REDIRECT_URI" = "https://$PUBLIC_HOST/api/v1/auth/social/callback/kakao" ] \
    || die "KAKAO_OAUTH_REDIRECT_URI가 공개 host의 exact callback과 다릅니다."

case "$APP_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "APP_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac
case "$FRONTEND_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "FRONTEND_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac
printf '%s' "$APP_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "APP_IMAGE_DIGEST는 sha256 digest여야 합니다."
printf '%s' "$FRONTEND_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "FRONTEND_IMAGE_DIGEST는 sha256 digest여야 합니다."

kube get crd certificates.cert-manager.io >/dev/null \
    || die "cert-manager가 설치되지 않았습니다. bootstrap-cluster.sh를 먼저 실행하세요."
kube get crd middlewares.traefik.io >/dev/null \
    || die "Traefik Middleware CRD를 찾을 수 없습니다."

for secret in happygallery-mysql happygallery-redis happygallery-app happygallery-alertmanager; do
    kube -n "$NAMESPACE" get secret "$secret" >/dev/null \
        || die "runtime Secret이 없습니다: $secret"
done

alert_config=$(kube -n "$NAMESPACE" get secret happygallery-alertmanager \
    -o 'jsonpath={.data.alertmanager\.yml}')
[ -n "$alert_config" ] || die "Alertmanager 설정이 없는 구형 Secret입니다. create-alertmanager-secret.sh로 먼저 갱신하세요."
unset alert_config

for image_and_digest in \
    "$APP_IMAGE|$APP_IMAGE_DIGEST" \
    "$FRONTEND_IMAGE|$FRONTEND_IMAGE_DIGEST"; do
    image=${image_and_digest%%|*}
    expected_digest=${image_and_digest#*|}
    containerd_has_image "$image" \
        || die "k3s containerd에 import되지 않은 이미지입니다: $image"
    actual_digest=$(containerd_image_digest "$image") \
        || die "k3s containerd 이미지 digest를 확인할 수 없습니다: $image"
    [ "$actual_digest" = "$expected_digest" ] \
        || die "$image digest가 release.env와 다릅니다: expected=$expected_digest actual=$actual_digest"
    containerd_has_image "$image@$expected_digest" \
        || die "digest 고정 이미지 참조가 containerd에 없습니다: $image@$expected_digest"
done

existing_mysql=$(kube -n "$NAMESPACE" get statefulset mysql --ignore-not-found -o name) \
    || die "기존 DB 조회에 실패했습니다. 백업 확인을 건너뛸 수 없습니다."
if [ -n "$existing_mysql" ]; then
    recovery_bundle=${VERIFIED_RECOVERY_BUNDLE_OVERRIDE:-$(require_env_value VERIFIED_RECOVERY_BUNDLE "$release_env")}
    verify_recovery_bundle_files "$recovery_bundle"
    recent=$(find "$recovery_bundle" -prune -mtime -2 -print)
    [ -n "$recent" ] || die "배포 전 백업이 48시간보다 오래됐습니다. 새 백업을 만드세요."
    info "배포 전 DB·미디어·호환 release 복구 묶음과 생성 시각을 확인했습니다."
fi

timestamp=$(date -u '+%Y%m%dT%H%M%SZ')
release_dir="$state_root/$timestamp-$IMAGE_TAG"
mkdir -p "$release_dir"
chmod 700 "$state_root" "$release_dir"
manifest="$release_dir/manifests.yaml"

"$SCRIPT_DIR/render-manifests.sh" "$manifest"
if [ "$phase" = prepare ]; then
    [ "$(ruby -r "$SCRIPT_DIR/gitops-release.rb" -e 'puts GitopsRelease.manifest_digest(ARGV.fetch(0))' "$manifest")" = "${GITOPS_MANIFEST_SHA256:-}" ] \
        || die "GitOps manifest와 서버 렌더링 결과가 다릅니다."
fi
cat > "$release_dir/metadata.env" <<EOF
PUBLIC_HOST=$PUBLIC_HOST
ACME_EMAIL=$ACME_EMAIL
APP_IMAGE=$APP_IMAGE
FRONTEND_IMAGE=$FRONTEND_IMAGE
APP_IMAGE_DIGEST=$APP_IMAGE_DIGEST
FRONTEND_IMAGE_DIGEST=$FRONTEND_IMAGE_DIGEST
IMAGE_TAG=$IMAGE_TAG
GOOGLE_OAUTH_REDIRECT_URI=$GOOGLE_OAUTH_REDIRECT_URI
NAVER_OAUTH_REDIRECT_URI=$NAVER_OAUTH_REDIRECT_URI
KAKAO_OAUTH_REDIRECT_URI=$KAKAO_OAUTH_REDIRECT_URI
APPLIED_AT=$timestamp
EOF
chmod 600 "$release_dir/metadata.env" "$manifest"

info "Kubernetes API에서 server-side dry-run을 실행합니다."
kube apply --dry-run=server -f "$manifest" >/dev/null
rolling_preflight
trap rolling_cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP

if [ "$phase" = prepare ]; then
    # 준비 실패는 workload 적용 전이므로 표식과 helper를 회수한다.
    trap 'rolling_cleanup; rm -f "$state_root/gitops-active"' EXIT
fi

info "이전·새 정적 파일을 보존하고 공개 경로를 먼저 확인합니다."
rolling_prepare_assets
info "HTTP 요청은 유지하고 교체 구간의 정기 배치 시작을 일시정지합니다."
rolling_pause_schedulers

if [ "$phase" = prepare ]; then
    declare -p PUBLIC_HOST ACME_EMAIL APP_IMAGE FRONTEND_IMAGE APP_IMAGE_DIGEST FRONTEND_IMAGE_DIGEST \
        IMAGE_TAG IMAGE_PULL_POLICY state_root timestamp release_dir manifest rolling_existing rolling_asset_helper \
        MEDIA_HELPER_POD rolling_guard_owner rolling_guard_created > "$release_dir/gitops-state.sh.tmp"
    chmod 600 "$release_dir/gitops-state.sh.tmp"
    mv "$release_dir/gitops-state.sh.tmp" "$release_dir/gitops-state.sh"
    printf '%s\n' "$release_dir" > "$state_root/gitops-active.tmp"
    mv "$state_root/gitops-active.tmp" "$state_root/gitops-active"
    trap - EXIT
    info "Argo CD 적용 준비 완료: $release_dir"
    exit 0
fi

info "release manifest를 적용합니다: $release_dir"
rolling_apply_started=true
kube apply -f "$manifest"
rolling_finish
