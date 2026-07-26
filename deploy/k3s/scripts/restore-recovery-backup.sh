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

backup_created_at=$(require_env_value BACKUP_CREATED_AT "$recovery_metadata")
database_file=$(require_env_value DATABASE_BACKUP "$recovery_metadata")
media_file=$(require_env_value MEDIA_BACKUP "$recovery_metadata")
image_tag=$(require_env_value IMAGE_TAG "$recovery_metadata")
release_relative=$(require_env_value RELEASE_DIR "$recovery_metadata")
expected_app_digest=$(require_env_value APP_IMAGE_DIGEST "$recovery_metadata")
expected_frontend_digest=$(require_env_value FRONTEND_IMAGE_DIGEST "$recovery_metadata")
expected_flyway_version=$(require_env_value FLYWAY_SCHEMA_VERSION "$recovery_metadata")
expected_key_id=$(require_env_value FIELD_ENCRYPTION_KEY_ID "$recovery_metadata")
expected_rotation_phase=$(require_env_value KEY_ROTATION_PHASE "$recovery_metadata")

printf '%s' "$backup_created_at" | grep -Eq '^[0-9]{8}T[0-9]{6}Z$' \
    || die "BACKUP_CREATED_AT 형식이 올바르지 않습니다."
recovery_metadata_sha256=$(sha256_file "$recovery_metadata")
reconciliation_token="$backup_created_at-$recovery_metadata_sha256"
release_state_root=${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}
restore_state_root=${HAPPYGALLERY_RESTORE_STATE_DIR:-$(dirname -- "$release_state_root")/restores}
umask 077
mkdir -p "$restore_state_root"
chmod 700 "$restore_state_root"
pending_marker="$restore_state_root/$reconciliation_token.pending"
activating_marker="$restore_state_root/$reconciliation_token.activating"
consumed_marker="$restore_state_root/$reconciliation_token.consumed"
restore_lock_dir="$restore_state_root/.restore-operation.lock"
restore_lock_owner="$restore_lock_dir/owner"
restore_lock_held=false
media_helper_started=false
pending_marker_tmp=

cleanup_restore() {
    status=$?
    trap - EXIT HUP INT TERM

    if [ "$media_helper_started" = true ]; then
        stop_media_helper
    fi
    if [ -n "$pending_marker_tmp" ] && ! rm -f "$pending_marker_tmp"; then
        printf '오류: 복원 대사 임시 파일을 정리하지 못했습니다: %s\n' "$pending_marker_tmp" >&2
        [ "$status" -ne 0 ] || status=1
    fi
    if [ "$restore_lock_held" = true ]; then
        if ! rm -f "$restore_lock_owner" || ! rmdir "$restore_lock_dir"; then
            printf '오류: 복원 실행 락을 정리하지 못했습니다. 수동 확인이 필요합니다: %s\n' "$restore_lock_dir" >&2
            [ "$status" -ne 0 ] || status=1
        fi
    fi

    exit "$status"
}
trap cleanup_restore EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

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

if ! mkdir "$restore_lock_dir" 2>/dev/null; then
    die "다른 복원이 실행 중이거나 이전 비정상 종료의 실행 락이 남아 있습니다. 실행 프로세스와 DB·미디어 상태를 확인한 뒤에만 락을 정리하세요: $restore_lock_dir"
fi
restore_lock_held=true
chmod 700 "$restore_lock_dir"
printf '%s\n' \
    "PID=$$" \
    "BACKUP_CREATED_AT=$backup_created_at" \
    "RECOVERY_METADATA_SHA256=$recovery_metadata_sha256" \
    > "$restore_lock_owner"
chmod 600 "$restore_lock_owner"

existing_unfinished_marker=$(find "$restore_state_root" -maxdepth 1 -type f \
    \( -name '*.pending' -o -name '*.activating' \) -print -quit)
[ -z "$existing_unfinished_marker" ] \
    || die "완료하지 않은 이전 복원 대사 상태가 있습니다. 해당 복원을 활성화하거나 운영 검토 후 정리하세요: $existing_unfinished_marker"
[ ! -e "$consumed_marker" ] \
    || die "이 복구 묶음의 대사 토큰은 이미 소비되었습니다: $reconciliation_token"
[ ! -e "$pending_marker" ] && [ ! -e "$activating_marker" ] \
    || die "이 복구 묶음의 대사 상태가 이미 존재합니다: $reconciliation_token"

"$SCRIPT_DIR/restore-mysql.sh" "$database_backup" "$identity" "$release_dir"

require_command age
require_command gzip
require_command tar
info "미디어 백업의 age 인증과 tar 스트림 무결성을 검사합니다."
age --decrypt -i "$identity" "$media_backup" | gzip -dc | tar -tf - >/dev/null

app_image=$(require_env_value APP_IMAGE "$release_metadata")
media_helper_started=true
start_media_helper "$app_image@$expected_app_digest"
info "상품 이미지 볼륨을 백업 시점으로 복원합니다."
kube -n "$NAMESPACE" exec "$(media_helper_pod_name)" -- \
    sh -ec 'find /media -mindepth 1 -depth -delete'
age --decrypt -i "$identity" "$media_backup" \
    | gzip -dc \
    | kube -n "$NAMESPACE" exec -i "$(media_helper_pod_name)" -- tar -C /media -xf -
stop_media_helper
media_helper_started=false

restored_flyway_version=$(kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
    exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -Nse \
      "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"
')
[ "$restored_flyway_version" = "$expected_flyway_version" ] \
    || die "복원 DB의 Flyway version이 백업 메타데이터와 다릅니다. app은 중지 상태로 유지합니다."

pending_marker_tmp=$(mktemp "$restore_state_root/.reconciliation.XXXXXX")
printf '%s\n' \
    "RESTORE_RECONCILIATION_TOKEN=$reconciliation_token" \
    "BACKUP_CREATED_AT=$backup_created_at" \
    "RECOVERY_METADATA_SHA256=$recovery_metadata_sha256" \
    "IMAGE_TAG=$image_tag" \
    "RELEASE_DIR=$release_dir" \
    > "$pending_marker_tmp"
chmod 600 "$pending_marker_tmp"
mv "$pending_marker_tmp" "$pending_marker"
pending_marker_tmp=

info "DB·미디어·키 ID·Flyway·release digest를 확인한 데이터 복원이 완료됐습니다."
info "app은 0 replica로 유지됩니다. PG·알림·개인정보 요청 대사를 마친 뒤 아래 일회용 대사 토큰으로 activate-restored-release.sh를 별도로 실행하세요."
info "복원 대사 토큰: $reconciliation_token"
