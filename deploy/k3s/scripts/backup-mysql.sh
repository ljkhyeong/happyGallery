#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

: "${BACKUP_DIR:?BACKUP_DIR가 필요합니다.}"
: "${BACKUP_AGE_RECIPIENT:?BACKUP_AGE_RECIPIENT가 필요합니다.}"

require_command age
require_command gzip
require_command base64
require_command tar
require_command ruby
marker=${BACKUP_TARGET_MARKER:-$BACKUP_DIR/.happygallery-off-device-backup-target}
[ -f "$marker" ] \
    || die "외부 백업 매체 marker가 없습니다. 매체가 실제로 mount됐는지 확인하세요: $marker"

kube -n "$NAMESPACE" get pod mysql-0 >/dev/null
kube -n "$NAMESPACE" wait --for=condition=Ready pod/mysql-0 --timeout=2m >/dev/null
kube -n "$NAMESPACE" get deployment app >/dev/null
original_app_replicas=$(kube -n "$NAMESPACE" get deployment app -o jsonpath='{.spec.replicas}')
case "$original_app_replicas" in
    0|1) ;;
    *) die "백업은 단일 app replica 구성에서만 실행할 수 있습니다: $original_app_replicas" ;;
esac

timestamp=$(date -u '+%Y%m%dT%H%M%SZ')
backup="$BACKUP_DIR/happygallery-$timestamp.sql.gz.age"
tmp="$backup.partial"
media_backup="$BACKUP_DIR/happygallery-$timestamp.media.tar.gz.age"
media_tmp="$media_backup.partial"
recovery_metadata="$BACKUP_DIR/happygallery-$timestamp.recovery.env"
recovery_metadata_tmp="$recovery_metadata.partial"
tmp_checksum="$tmp.sha256"
media_tmp_checksum="$media_tmp.sha256"
recovery_metadata_tmp_checksum="$recovery_metadata_tmp.sha256"
release_state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
current_release=$(CDPATH= cd -- "$release_state_root/current" 2>/dev/null && pwd) \
    || die "현재 release 메타데이터를 찾을 수 없습니다: $release_state_root/current"
release_metadata="$current_release/metadata.env"
release_manifest="$current_release/manifests.yaml"
validate_env_file "$release_metadata"
[ -f "$release_manifest" ] || die "현재 release manifest를 찾을 수 없습니다: $release_manifest"

APP_IMAGE=$(require_env_value APP_IMAGE "$release_metadata")
FRONTEND_IMAGE=$(require_env_value FRONTEND_IMAGE "$release_metadata")
APP_IMAGE_DIGEST=$(require_env_value APP_IMAGE_DIGEST "$release_metadata")
FRONTEND_IMAGE_DIGEST=$(require_env_value FRONTEND_IMAGE_DIGEST "$release_metadata")
IMAGE_TAG=$(require_env_value IMAGE_TAG "$release_metadata")
release_backup_root="$BACKUP_DIR/releases"
release_backup="$release_backup_root/$IMAGE_TAG"
release_tmp="$release_backup.partial.$timestamp"
MYSQL_IMAGE=
REDIS_IMAGE=
PROMETHEUS_IMAGE=
ALERTMANAGER_IMAGE=
GRAFANA_IMAGE=
runtime_images=$(ruby "$SCRIPT_DIR/runtime-images-from-manifest.rb" "$release_manifest")
while IFS='=' read -r key value; do
    case "$key" in
        MYSQL_IMAGE) MYSQL_IMAGE=$value ;;
        REDIS_IMAGE) REDIS_IMAGE=$value ;;
        PROMETHEUS_IMAGE) PROMETHEUS_IMAGE=$value ;;
        ALERTMANAGER_IMAGE) ALERTMANAGER_IMAGE=$value ;;
        GRAFANA_IMAGE) GRAFANA_IMAGE=$value ;;
        *) die "알 수 없는 runtime image 항목입니다: $key" ;;
    esac
done <<< "$runtime_images"
for runtime_image in \
    "$MYSQL_IMAGE" "$REDIS_IMAGE" "$PROMETHEUS_IMAGE" "$ALERTMANAGER_IMAGE" "$GRAFANA_IMAGE"; do
    [ -n "$runtime_image" ] || die "release manifest에서 runtime image를 모두 확인하지 못했습니다."
    containerd_has_image "$runtime_image" \
        || die "외부 복구 archive에 넣을 runtime 이미지를 찾을 수 없습니다: $runtime_image"
done
mysql_image_digest=$(containerd_image_digest "$MYSQL_IMAGE")
redis_image_digest=$(containerd_image_digest "$REDIS_IMAGE")
prometheus_image_digest=$(containerd_image_digest "$PROMETHEUS_IMAGE")
alertmanager_image_digest=$(containerd_image_digest "$ALERTMANAGER_IMAGE")
grafana_image_digest=$(containerd_image_digest "$GRAFANA_IMAGE")
for runtime_digest in \
    "$mysql_image_digest" "$redis_image_digest" \
    "$prometheus_image_digest" "$alertmanager_image_digest" "$grafana_image_digest"; do
    printf '%s' "$runtime_digest" | grep -Eq '^sha256:[a-f0-9]{64}$' \
        || die "runtime 이미지 digest가 올바르지 않습니다: $runtime_digest"
done
umask 077
for target in \
    "$backup" "$backup.sha256" "$media_backup" "$media_backup.sha256" \
    "$recovery_metadata" "$recovery_metadata.sha256"; do
    [ ! -e "$target" ] || die "같은 시각의 백업 파일이 이미 존재합니다: $target"
done
rm -f \
    "$tmp" "$tmp_checksum" \
    "$media_tmp" "$media_tmp_checksum" \
    "$recovery_metadata_tmp" "$recovery_metadata_tmp_checksum"
rm -rf "$release_tmp"
media_helper_started=false
app_restore_required=false

restore_app() {
    [ "$app_restore_required" = true ] || return 0
    info "백업 전 app replica를 복구합니다: $original_app_replicas"
    kube -n "$NAMESPACE" scale deployment/app --replicas="$original_app_replicas" >/dev/null
    if [ "$original_app_replicas" -eq 1 ]; then
        kube -n "$NAMESPACE" rollout status deployment/app --timeout=8m
    fi
    app_restore_required=false
}

cleanup_partial_backup() {
    if [ "$media_helper_started" = true ]; then
        stop_media_helper
    fi
    rm -f \
        "$tmp" "$tmp_checksum" \
        "$media_tmp" "$media_tmp_checksum" \
        "$recovery_metadata_tmp" "$recovery_metadata_tmp_checksum"
    rm -rf "$release_tmp"
    restore_app
}
trap cleanup_partial_backup EXIT HUP INT TERM

if [ ! -d "$release_backup" ]; then
    info "현재 release 이미지와 메타데이터를 외부 매체에 보존합니다: $IMAGE_TAG"
    mkdir -p "$release_backup_root" "$release_tmp"
    cp "$release_metadata" "$release_tmp/metadata.env"
    cp "$release_manifest" "$release_tmp/manifests.yaml"
    printf '%s  %s\n' "$(sha256_file "$release_tmp/metadata.env")" "metadata.env" \
        > "$release_tmp/metadata.env.sha256"
    printf '%s  %s\n' "$(sha256_file "$release_tmp/manifests.yaml")" "manifests.yaml" \
        > "$release_tmp/manifests.yaml.sha256"
    mysql_archive_image=$(normalize_image_reference "$MYSQL_IMAGE")
    redis_archive_image=$(normalize_image_reference "$REDIS_IMAGE")
    prometheus_archive_image=$(normalize_image_reference "$PROMETHEUS_IMAGE")
    alertmanager_archive_image=$(normalize_image_reference "$ALERTMANAGER_IMAGE")
    grafana_archive_image=$(normalize_image_reference "$GRAFANA_IMAGE")
    cat > "$release_tmp/runtime-images.env" <<EOF
MYSQL_IMAGE=$MYSQL_IMAGE
MYSQL_IMAGE_DIGEST=$mysql_image_digest
REDIS_IMAGE=$REDIS_IMAGE
REDIS_IMAGE_DIGEST=$redis_image_digest
PROMETHEUS_IMAGE=$PROMETHEUS_IMAGE
PROMETHEUS_IMAGE_DIGEST=$prometheus_image_digest
ALERTMANAGER_IMAGE=$ALERTMANAGER_IMAGE
ALERTMANAGER_IMAGE_DIGEST=$alertmanager_image_digest
GRAFANA_IMAGE=$GRAFANA_IMAGE
GRAFANA_IMAGE_DIGEST=$grafana_image_digest
EOF
    printf '%s  %s\n' "$(sha256_file "$release_tmp/runtime-images.env")" "runtime-images.env" \
        > "$release_tmp/runtime-images.env.sha256"
    images_archive="$release_tmp/images.tar"
    k3s_ctr images export "$images_archive" \
        "$APP_IMAGE@$APP_IMAGE_DIGEST" \
        "$FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST" \
        "$mysql_archive_image" "$redis_archive_image" \
        "$prometheus_archive_image" "$alertmanager_archive_image" \
        "$grafana_archive_image"
    [ -s "$images_archive" ] || die "release 이미지 archive가 비어 있습니다."
    printf '%s  %s\n' "$(sha256_file "$images_archive")" "images.tar" \
        > "$images_archive.sha256"
    chmod 600 "$release_tmp"/*
    mv "$release_tmp" "$release_backup"
else
    verify_checksum "$release_backup/metadata.env"
    verify_checksum "$release_backup/manifests.yaml"
    verify_checksum "$release_backup/runtime-images.env"
    validate_env_file "$release_backup/metadata.env"
    [ "$(require_env_value APP_IMAGE_DIGEST "$release_backup/metadata.env")" = "$APP_IMAGE_DIGEST" ] \
        || die "외부 release의 app digest가 현재 release와 다릅니다: $release_backup"
    [ "$(require_env_value FRONTEND_IMAGE_DIGEST "$release_backup/metadata.env")" = "$FRONTEND_IMAGE_DIGEST" ] \
        || die "외부 release의 frontend digest가 현재 release와 다릅니다: $release_backup"
    verify_archived_runtime_image() {
        prefix=$1
        current_image=$2
        current_digest=$3
        archived_image=$(require_env_value "${prefix}_IMAGE" "$release_backup/runtime-images.env")
        archived_digest=$(require_env_value "${prefix}_IMAGE_DIGEST" "$release_backup/runtime-images.env")
        [ "$archived_image" = "$current_image" ] \
            || die "외부 release의 $prefix 이미지가 현재 release manifest와 다릅니다: $release_backup"
        [ "$archived_digest" = "$current_digest" ] \
            || die "외부 release의 $prefix digest가 현재 containerd와 다릅니다: $release_backup"
    }
    verify_archived_runtime_image MYSQL "$MYSQL_IMAGE" "$mysql_image_digest"
    verify_archived_runtime_image REDIS "$REDIS_IMAGE" "$redis_image_digest"
    verify_archived_runtime_image PROMETHEUS "$PROMETHEUS_IMAGE" "$prometheus_image_digest"
    verify_archived_runtime_image ALERTMANAGER "$ALERTMANAGER_IMAGE" "$alertmanager_image_digest"
    verify_archived_runtime_image GRAFANA "$GRAFANA_IMAGE" "$grafana_image_digest"
    verify_checksum "$release_backup/images.tar"
fi

flyway_schema_version=$(kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -Nse \
      "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"
')
[ -n "$flyway_schema_version" ] || die "Flyway schema version을 확인할 수 없습니다."

field_key_id_encoded=$(kube -n "$NAMESPACE" get secret happygallery-app \
    -o 'jsonpath={.data.FIELD_ENCRYPTION_KEY_ID}')
field_key_id=$(printf '%s' "$field_key_id_encoded" | decode_base64)
[ -n "$field_key_id" ] || die "FIELD_ENCRYPTION_KEY_ID를 확인할 수 없습니다."
key_rotation_phase=$(kube -n "$NAMESPACE" get secret happygallery-app \
    -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-phase}' 2>/dev/null || true)
[ -n "$key_rotation_phase" ] || key_rotation_phase=none

secret_fingerprint() {
    secret_key=$1
    encoded=$(kube -n "$NAMESPACE" get secret happygallery-app \
        -o "jsonpath={.data.$secret_key}")
    printf '%s' "$encoded" | decode_base64 | sha256_stream
}
encrypt_key_sha256=$(secret_fingerprint ENCRYPT_KEY)
hmac_key_sha256=$(secret_fingerprint HMAC_KEY)
previous_encrypt_keys_sha256=$(secret_fingerprint PREVIOUS_ENCRYPT_KEYS)
previous_hmac_keys_sha256=$(secret_fingerprint PREVIOUS_HMAC_KEYS)
guest_token_key_sha256=$(secret_fingerprint GUEST_TOKEN_HMAC_SECRET)
guest_token_previous_key_sha256=$(secret_fingerprint GUEST_TOKEN_PREVIOUS_HMAC_SECRET)

if [ "$original_app_replicas" -eq 1 ]; then
    info "DB와 미디어를 같은 쓰기 중단 구간에 보관하기 위해 app을 0 replica로 축소합니다."
    app_restore_required=true
    kube -n "$NAMESPACE" scale deployment/app --replicas=0 >/dev/null
    wait_for_no_pods "$NAMESPACE" 'app.kubernetes.io/name=app' 120
fi

info "MySQL 논리 무결성 검사를 실행합니다."
kube -n "$NAMESPACE" exec mysql-0 -- sh -ec \
    'exec mysqlcheck --check --all-databases -uroot -p"$MYSQL_ROOT_PASSWORD"' >/dev/null

info "MySQL 백업을 외부 매체에 age 암호화합니다."
kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysqldump \
      --single-transaction \
      --routines \
      --events \
      --triggers \
      --hex-blob \
      --no-tablespaces \
      --set-gtid-purged=OFF \
      --databases "$MYSQL_DATABASE" \
      -uroot -p"$MYSQL_ROOT_PASSWORD"
' | gzip -9 | age -r "$BACKUP_AGE_RECIPIENT" -o "$tmp"

[ -s "$tmp" ] || die "생성된 백업 파일이 비어 있습니다."
checksum=$(sha256_file "$tmp")
printf '%s  %s\n' "$checksum" "$(basename -- "$backup")" > "$tmp_checksum"
chmod 600 "$tmp" "$tmp_checksum"

info "상품 이미지 볼륨을 외부 매체에 age 암호화합니다."
ensure_media_pvc
media_helper_started=true
start_media_helper "$APP_IMAGE@$APP_IMAGE_DIGEST"
kube -n "$NAMESPACE" exec "$(media_helper_pod_name)" -- tar -C /media -cf - . \
    | gzip -9 \
    | age -r "$BACKUP_AGE_RECIPIENT" -o "$media_tmp"
[ -s "$media_tmp" ] || die "생성된 미디어 백업 파일이 비어 있습니다."
printf '%s  %s\n' "$(sha256_file "$media_tmp")" "$(basename -- "$media_backup")" \
    > "$media_tmp_checksum"
chmod 600 "$media_tmp" "$media_tmp_checksum"
stop_media_helper
media_helper_started=false
restore_app

cat > "$recovery_metadata_tmp" <<EOF
BACKUP_CREATED_AT=$timestamp
DATABASE_BACKUP=$(basename -- "$backup")
MEDIA_BACKUP=$(basename -- "$media_backup")
RELEASE_DIR=releases/$IMAGE_TAG
IMAGE_TAG=$IMAGE_TAG
APP_IMAGE_DIGEST=$APP_IMAGE_DIGEST
FRONTEND_IMAGE_DIGEST=$FRONTEND_IMAGE_DIGEST
FLYWAY_SCHEMA_VERSION=$flyway_schema_version
FIELD_ENCRYPTION_KEY_ID=$field_key_id
KEY_ROTATION_PHASE=$key_rotation_phase
ENCRYPT_KEY_SHA256=$encrypt_key_sha256
HMAC_KEY_SHA256=$hmac_key_sha256
PREVIOUS_ENCRYPT_KEYS_SHA256=$previous_encrypt_keys_sha256
PREVIOUS_HMAC_KEYS_SHA256=$previous_hmac_keys_sha256
GUEST_TOKEN_HMAC_SECRET_SHA256=$guest_token_key_sha256
GUEST_TOKEN_PREVIOUS_HMAC_SECRET_SHA256=$guest_token_previous_key_sha256
EOF
printf '%s  %s\n' "$(sha256_file "$recovery_metadata_tmp")" "$(basename -- "$recovery_metadata")" \
    > "$recovery_metadata_tmp_checksum"
chmod 600 "$recovery_metadata_tmp" "$recovery_metadata_tmp_checksum"

# recovery.env가 보일 때는 모든 archive와 sidecar가 이미 완성된 상태여야 한다.
mv "$tmp" "$backup"
mv "$tmp_checksum" "$backup.sha256"
mv "$media_tmp" "$media_backup"
mv "$media_tmp_checksum" "$media_backup.sha256"
mv "$recovery_metadata_tmp_checksum" "$recovery_metadata.sha256"
mv "$recovery_metadata_tmp" "$recovery_metadata"

trap - EXIT HUP INT TERM

info "암호화 DB·미디어와 호환 release 복구 묶음 완료: $recovery_metadata"
printf '%s\n' "$backup"
