#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 2 ] || die "사용법: $0 <backup.recovery.env> <age identity 파일>"
recovery_metadata=$(CDPATH= cd -- "$(dirname -- "$1")" 2>/dev/null && pwd)/$(basename -- "$1")
identity=$2
require_private_file "$recovery_metadata"
verify_checksum "$recovery_metadata"
validate_env_file "$recovery_metadata"
require_command base64

database_file=$(require_env_value DATABASE_BACKUP "$recovery_metadata")
media_file=$(require_env_value MEDIA_BACKUP "$recovery_metadata")
image_tag=$(require_env_value IMAGE_TAG "$recovery_metadata")
release_relative=$(require_env_value RELEASE_DIR "$recovery_metadata")
expected_app_digest=$(require_env_value APP_IMAGE_DIGEST "$recovery_metadata")
expected_frontend_digest=$(require_env_value FRONTEND_IMAGE_DIGEST "$recovery_metadata")
expected_flyway_version=$(require_env_value FLYWAY_SCHEMA_VERSION "$recovery_metadata")
expected_key_id=$(require_env_value FIELD_ENCRYPTION_KEY_ID "$recovery_metadata")
expected_rotation_phase=$(require_env_value KEY_ROTATION_PHASE "$recovery_metadata")

case "$database_file" in
    */*|.|..) die "DATABASE_BACKUP은 복구 메타데이터와 같은 디렉터리의 파일명이어야 합니다." ;;
esac
case "$media_file" in
    */*|.|..) die "MEDIA_BACKUP은 복구 메타데이터와 같은 디렉터리의 파일명이어야 합니다." ;;
esac
[ "$release_relative" = "releases/$image_tag" ] \
    || die "RELEASE_DIR은 IMAGE_TAG와 일치하는 releases/<IMAGE_TAG> 형식이어야 합니다."

backup_root=$(dirname -- "$recovery_metadata")
database_backup="$backup_root/$database_file"
media_backup="$backup_root/$media_file"
release_dir="$backup_root/$release_relative"
release_metadata="$release_dir/metadata.env"
release_manifest="$release_dir/manifests.yaml"
runtime_images_metadata="$release_dir/runtime-images.env"
images_archive="$release_dir/images.tar"
[ -f "$database_backup" ] || die "DB 백업을 찾을 수 없습니다: $database_backup"
[ -f "$media_backup" ] || die "미디어 백업을 찾을 수 없습니다: $media_backup"
verify_checksum "$database_backup"
verify_checksum "$media_backup"
verify_checksum "$release_metadata"
verify_checksum "$release_manifest"
verify_checksum "$runtime_images_metadata"
verify_checksum "$images_archive"
validate_env_file "$release_metadata"
validate_env_file "$runtime_images_metadata"
[ "$(require_env_value APP_IMAGE_DIGEST "$release_metadata")" = "$expected_app_digest" ] \
    || die "복구 메타데이터와 release app digest가 다릅니다."
[ "$(require_env_value FRONTEND_IMAGE_DIGEST "$release_metadata")" = "$expected_frontend_digest" ] \
    || die "복구 메타데이터와 release frontend digest가 다릅니다."

runtime_key_id_encoded=$(kube -n "$NAMESPACE" get secret happygallery-app \
    -o 'jsonpath={.data.FIELD_ENCRYPTION_KEY_ID}')
runtime_key_id=$(printf '%s' "$runtime_key_id_encoded" | decode_base64)
[ "$runtime_key_id" = "$expected_key_id" ] \
    || die "runtime FIELD_ENCRYPTION_KEY_ID가 백업과 다릅니다. 올바른 복구 키링을 먼저 구성하세요."
runtime_rotation_phase=$(kube -n "$NAMESPACE" get secret happygallery-app \
    -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-phase}' 2>/dev/null || true)
[ -n "$runtime_rotation_phase" ] || runtime_rotation_phase=none
[ "$runtime_rotation_phase" = "$expected_rotation_phase" ] \
    || die "runtime 키 회전 단계가 백업과 다릅니다. 백업 시점 Secret annotation을 먼저 복구하세요."

verify_secret_fingerprint() {
    secret_key=$1
    metadata_key=$2
    expected_fingerprint=$(require_env_value "$metadata_key" "$recovery_metadata")
    encoded=$(kube -n "$NAMESPACE" get secret happygallery-app \
        -o "jsonpath={.data.$secret_key}")
    actual_fingerprint=$(printf '%s' "$encoded" | decode_base64 | sha256_stream)
    [ "$actual_fingerprint" = "$expected_fingerprint" ] \
        || die "runtime $secret_key 값이 백업 keyring과 다릅니다. 올바른 복구 키를 먼저 구성하세요."
}
verify_secret_fingerprint ENCRYPT_KEY ENCRYPT_KEY_SHA256
verify_secret_fingerprint HMAC_KEY HMAC_KEY_SHA256
verify_secret_fingerprint PREVIOUS_ENCRYPT_KEYS PREVIOUS_ENCRYPT_KEYS_SHA256
verify_secret_fingerprint PREVIOUS_HMAC_KEYS PREVIOUS_HMAC_KEYS_SHA256
verify_secret_fingerprint GUEST_TOKEN_HMAC_SECRET GUEST_TOKEN_HMAC_SECRET_SHA256
verify_secret_fingerprint GUEST_TOKEN_PREVIOUS_HMAC_SECRET GUEST_TOKEN_PREVIOUS_HMAC_SECRET_SHA256

"$SCRIPT_DIR/restore-mysql.sh" "$database_backup" "$identity" "$release_dir"

require_command age
require_command gzip
require_command tar
info "미디어 백업의 age 인증과 tar 스트림 무결성을 검사합니다."
age --decrypt -i "$identity" "$media_backup" | gzip -dc | tar -tf - >/dev/null

app_image=$(require_env_value APP_IMAGE "$release_metadata")
media_helper_started=true
cleanup_media_helper() {
    if [ "$media_helper_started" = true ]; then
        stop_media_helper
    fi
}
trap cleanup_media_helper EXIT HUP INT TERM
start_media_helper "$app_image@$expected_app_digest"
info "상품 이미지 볼륨을 백업 시점으로 복원합니다."
kube -n "$NAMESPACE" exec "$(media_helper_pod_name)" -- \
    sh -ec 'find /media -mindepth 1 -depth -delete'
age --decrypt -i "$identity" "$media_backup" \
    | gzip -dc \
    | kube -n "$NAMESPACE" exec -i "$(media_helper_pod_name)" -- tar -C /media -xf -
stop_media_helper
media_helper_started=false
trap - EXIT HUP INT TERM

restored_flyway_version=$(kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -Nse \
      "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"
')
[ "$restored_flyway_version" = "$expected_flyway_version" ] \
    || die "복원 DB의 Flyway version이 백업 메타데이터와 다릅니다. app은 중지 상태로 유지합니다."

"$SCRIPT_DIR/activate-restored-release.sh" "$release_dir"
info "DB·미디어·키 ID·Flyway·release digest를 확인한 복구가 완료됐습니다. verify.sh를 실행하세요."
