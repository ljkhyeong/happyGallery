#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <호환 release 디렉터리>"
release_dir=$(CDPATH= cd -- "$1" 2>/dev/null && pwd) \
    || die "호환 release 디렉터리를 찾을 수 없습니다: $1"
metadata="$release_dir/metadata.env"
runtime_metadata="$release_dir/runtime-images.env"
images_archive="$release_dir/images.tar"

verify_checksum "$metadata"
verify_checksum "$runtime_metadata"
verify_checksum "$images_archive"
validate_env_file "$metadata"
validate_env_file "$runtime_metadata"

APP_IMAGE=$(require_env_value APP_IMAGE "$metadata")
FRONTEND_IMAGE=$(require_env_value FRONTEND_IMAGE "$metadata")
APP_IMAGE_DIGEST=$(require_env_value APP_IMAGE_DIGEST "$metadata")
FRONTEND_IMAGE_DIGEST=$(require_env_value FRONTEND_IMAGE_DIGEST "$metadata")
IMAGE_TAG=$(require_env_value IMAGE_TAG "$metadata")

printf '%s' "$APP_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "보존된 app 이미지 digest가 올바르지 않습니다."
printf '%s' "$FRONTEND_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "보존된 frontend 이미지 digest가 올바르지 않습니다."
printf '%s' "$IMAGE_TAG" | grep -Eq '^[A-Fa-f0-9]{12,40}$' \
    || die "보존된 IMAGE_TAG가 올바르지 않습니다."
case "$APP_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "보존된 APP_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac
case "$FRONTEND_IMAGE" in
    *:"$IMAGE_TAG") ;;
    *) die "보존된 FRONTEND_IMAGE 태그가 IMAGE_TAG와 다릅니다." ;;
esac

app_ref="$APP_IMAGE@$APP_IMAGE_DIGEST"
frontend_ref="$FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST"

all_required_images_match() {
    containerd_has_image "$app_ref" || return 1
    containerd_has_image "$frontend_ref" || return 1

    for prefix in MYSQL REDIS PROMETHEUS ALERTMANAGER GRAFANA; do
        runtime_image=$(require_env_value "${prefix}_IMAGE" "$runtime_metadata")
        expected_digest=$(require_env_value "${prefix}_IMAGE_DIGEST" "$runtime_metadata")
        printf '%s' "$expected_digest" | grep -Eq '^sha256:[a-f0-9]{64}$' || return 1
        actual_digest=$(containerd_image_digest "$runtime_image") || return 1
        [ "$actual_digest" = "$expected_digest" ] || return 1
    done
}

if ! all_required_images_match; then
    info "외부 복구 묶음의 app/frontend/runtime 이미지를 k3s containerd에 가져옵니다."
    k3s_ctr images import "$images_archive"
fi

all_required_images_match \
    || die "복구 archive import 후 필수 app/frontend/runtime 이미지 digest를 확인하지 못했습니다."
info "복구 release의 모든 이미지 digest를 확인했습니다: $IMAGE_TAG"
