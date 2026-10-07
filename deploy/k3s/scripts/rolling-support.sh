#!/usr/bin/env sh

# rollout.sh에서 source한다. 실패 시에도 기존 HTTP pod는 삭제하거나 축소하지 않는다.
rolling_preflight() {
    rolling_existing=false
    rolling_schema_reset=
    rolling_current=$(kube -n "$NAMESPACE" get deployment app --ignore-not-found -o name) \
        || die "현재 앱 조회에 실패했습니다. 첫 배포로 간주하지 않습니다."
    if [ -n "$rolling_current" ]; then
        rolling_existing=true
        [ -f "$state_root/current/manifests.yaml" ] || die "현재 release manifest가 없어 롤링 호환성을 비교할 수 없습니다."
        rolling_check=$(ruby "$SCRIPT_DIR/rolling-release.rb" check "$REPO_ROOT" "$state_root/current/manifests.yaml" "$manifest")
        rolling_schema_reset=$(printf '%s\n' "$rolling_check" | sed -n 's/^SCHEMA_RESET=//p')
        for rolling_name in app frontend; do
            rolling_expected=$(ruby -r "$SCRIPT_DIR/rolling-release.rb" -e \
                'puts RollingRelease.workload(RollingRelease.documents(ARGV[0]), ARGV[1]).dig("spec", "template", "spec", "containers", 0, "image")' \
                "$state_root/current/manifests.yaml" "$rolling_name")
            rolling_live=$(kube -n "$NAMESPACE" get deployment "$rolling_name" -o 'jsonpath={.spec.template.spec.containers[0].image}')
            [ "$rolling_expected" = "$rolling_live" ] || die "$rolling_name 실제 이미지가 현재 release 기록과 다릅니다."
            kube -n "$NAMESPACE" rollout status "deployment/$rolling_name" --timeout=30s
        done
    fi
    # 남아 있는 확인 변수로 다른 release에서 DB를 비우지 않도록 감지한 표식과 정확히 같을 때만 진행한다.
    if [ -n "${HAPPYGALLERY_SCHEMA_RESET:-}" ] && [ "$rolling_schema_reset" != "$HAPPYGALLERY_SCHEMA_RESET" ]; then
        die "HAPPYGALLERY_SCHEMA_RESET=$HAPPYGALLERY_SCHEMA_RESET 와 일치하는 스키마 초기화 release가 아닙니다."
    fi
    # 기준선 migration은 빈 DB에서만 성공한다. 기존 테이블이 남아 있으면 앱을 교체하기 전에 멈춘다.
    if [ -n "$rolling_schema_reset" ]; then
        rolling_tables=$(kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
            exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse \
              "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '\''happygallery'\''"
        ') || die "스키마 초기화 대상 DB를 확인할 수 없습니다."
        [ "$rolling_tables" = 0 ] \
            || die "스키마 초기화 release는 빈 happygallery DB에만 배포합니다. 현재 테이블 수: $rolling_tables"
    fi
    # local-path RWO 공유와 파일 잠금은 한 노드에서만 보장한다.
    rolling_nodes=$(kube get nodes -o 'jsonpath={range .items[*]}{.metadata.name}{"\n"}{end}')
    [ -n "$rolling_nodes" ] && [ "$(printf '%s\n' "$rolling_nodes" | wc -l | tr -d ' ')" -eq 1 ] \
        || die "이 롤링 배포는 단일 노드 k3s 전용입니다."
}

rolling_prepare_assets() {
    ruby "$SCRIPT_DIR/rolling-release.rb" assets "$manifest" storage > "$release_dir/assets-storage.yaml"
    kube apply -f "$release_dir/assets-storage.yaml" >/dev/null
    rolling_asset_helper="assets-publish-$(date +%s)-$$"
    kube -n "$NAMESPACE" apply -f - >/dev/null <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $rolling_asset_helper
spec:
  restartPolicy: Never
  automountServiceAccountToken: false
  securityContext:
    runAsNonRoot: true
    runAsUser: 101
    runAsGroup: 101
    fsGroup: 101
    seccompProfile: {type: RuntimeDefault}
  containers:
    - name: publisher
      image: $FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST
      imagePullPolicy: Never
      command: [sh, -ec, "sleep 3600"]
      resources:
        requests: {cpu: 25m, memory: 32Mi}
        limits: {cpu: 250m, memory: 128Mi}
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities: {drop: [ALL]}
      volumeMounts:
        - {name: assets, mountPath: /assets}
        - {name: tmp, mountPath: /tmp}
  volumes:
    - name: assets
      persistentVolumeClaim: {claimName: frontend-assets}
    - name: tmp
      emptyDir: {sizeLimit: 256Mi}
EOF
    kube -n "$NAMESPACE" wait --for=condition=Ready "pod/$rolling_asset_helper" --timeout=2m >/dev/null
    if [ "$rolling_existing" = true ]; then
        # pipe 대신 먼저 완전한 archive를 받아 생산자 실패도 확인한다.
        kube -n "$NAMESPACE" exec deployment/frontend -c frontend -- \
            tar -C /app/build/client/assets -cf - . > "$release_dir/previous-assets.tar"
        kube -n "$NAMESPACE" exec -i "$rolling_asset_helper" -- sh -ec \
            'mkdir /tmp/previous; tar -C /tmp/previous -xf -; node /app/publish-assets.mjs /tmp/previous /assets' \
            < "$release_dir/previous-assets.tar"
        rm "$release_dir/previous-assets.tar"
    fi
    kube -n "$NAMESPACE" exec "$rolling_asset_helper" -- node /app/publish-assets.mjs /app/build/client/assets /assets
    ruby "$SCRIPT_DIR/rolling-release.rb" assets "$manifest" server > "$release_dir/assets-server.yaml"
    kube apply -f "$release_dir/assets-server.yaml" >/dev/null
    kube -n "$NAMESPACE" rollout status deployment/frontend-assets --timeout=3m
    if [ "$rolling_existing" = true ]; then
        ruby "$SCRIPT_DIR/rolling-release.rb" assets "$manifest" route > "$release_dir/assets-route.yaml"
        kube apply -f "$release_dir/assets-route.yaml" >/dev/null
        rolling_attempt=0
        until [ "$(curl -fsS --connect-timeout 3 --max-time 5 "https://$PUBLIC_HOST/assets/happygallery-asset-store-v1.txt" 2>/dev/null || true)" = shared-assets-v1 ]; do
            rolling_attempt=$((rolling_attempt + 1))
            [ "$rolling_attempt" -lt 12 ] || die "정적 파일 공개 경로를 확인하지 못했습니다. 앱은 교체하지 않았습니다."
            sleep 2
        done
    fi
}

rolling_pause_schedulers() {
    MEDIA_HELPER_POD="rolling-media-$(date +%s)-$$"
    export MEDIA_HELPER_POD
    ensure_media_pvc
    start_media_helper "$APP_IMAGE@$APP_IMAGE_DIGEST"
    kube -n "$NAMESPACE" exec "$MEDIA_HELPER_POD" -- test -f /app/rolling-deployment-v1
    rolling_guard_owner="$timestamp-$$"
    kube -n "$NAMESPACE" exec "$MEDIA_HELPER_POD" -- sh -ec \
        'mkdir /media/.deployment-in-progress; printf "%s\n" "$1" > /media/.deployment-in-progress/owner' sh "$rolling_guard_owner"
    rolling_guard_created=true
    if [ "$rolling_existing" = true ]; then
        kube -n "$NAMESPACE" get deployment app -o 'jsonpath={.spec.template}' > "$release_dir/previous-app-template.json"
    fi
    kube -n "$NAMESPACE" get pods -l app.kubernetes.io/name=app -o name > "$release_dir/previous-app-pods.txt"
}

rolling_wait_old_pods() {
    if [ -f "$release_dir/previous-app-template.json" ]; then
        kube -n "$NAMESPACE" get deployment app -o 'jsonpath={.spec.template}' > "$release_dir/applied-app-template.json"
        if cmp -s "$release_dir/previous-app-template.json" "$release_dir/applied-app-template.json"; then
            rolling_old_gone=true
            return
        fi
    fi
    while IFS= read -r rolling_old_pod; do
        [ -n "$rolling_old_pod" ] || continue
        kube -n "$NAMESPACE" wait --for=delete "$rolling_old_pod" --timeout=2m
    done < "$release_dir/previous-app-pods.txt"
    rolling_old_gone=true
}

rolling_resume_schedulers() {
    kube -n "$NAMESPACE" exec "$MEDIA_HELPER_POD" -- sh -ec \
        '[ "$(cat /media/.deployment-in-progress/owner)" = "$1" ]; rm /media/.deployment-in-progress/owner; rmdir /media/.deployment-in-progress' \
        sh "$rolling_guard_owner" || return 1
    rolling_guard_created=false
}

rolling_cleanup() {
    # 실패한 혼합 상태에서는 legacy 배치가 남아 있을 수 있어 새 배치를 재개하지 않는다.
    if [ "${rolling_guard_created:-false}" = true ]; then
        if [ "${rolling_apply_started:-false}" = false ] || [ "${rolling_old_gone:-false}" = true ]; then
            rolling_resume_schedulers || info "배치 일시정지 표식 해제 실패: 수동 확인이 필요합니다."
        else
            info "배포 실패: 배치 일시정지 표식을 유지합니다. 구버전 pod 종료를 확인한 뒤 해제하세요."
        fi
    fi
    [ -z "${MEDIA_HELPER_POD:-}" ] || stop_media_helper
    if [ -n "${rolling_asset_helper:-}" ]; then
        kube -n "$NAMESPACE" delete pod "$rolling_asset_helper" --ignore-not-found --wait=false >/dev/null 2>&1 || true
    fi
}
