#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <호환 release 디렉터리>"
release_dir=$(CDPATH= cd -- "$1" 2>/dev/null && pwd) \
    || die "호환 release 디렉터리를 찾을 수 없습니다: $1"
metadata="$release_dir/metadata.env"
manifest="$release_dir/manifests.yaml"
runtime_metadata="$release_dir/runtime-images.env"
images_archive="$release_dir/images.tar"

require_command ruby
verify_checksum "$metadata"
verify_checksum "$manifest"
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
app_cri_ref="${APP_IMAGE%:*}@$APP_IMAGE_DIGEST"
frontend_cri_ref="${FRONTEND_IMAGE%:*}@$FRONTEND_IMAGE_DIGEST"
runtime_inventory=$(ruby "$SCRIPT_DIR/runtime-images-from-manifest.rb" \
    --inventory "$manifest" "$runtime_metadata")

# import가 기존 별칭을 덮어쓰기 전에 네 이름의 충돌을 먼저 확인한다.
for reference in "$app_ref" "$frontend_ref" "$app_cri_ref" "$frontend_cri_ref"; do
    if containerd_has_image "$reference"; then
        actual_digest=$(containerd_image_digest "$reference")
        [ "$actual_digest" = "${reference#*@}" ] \
            || die "기존 복구 이미지 별칭의 digest가 다릅니다: $reference"
    fi
done

all_required_images_match() {
    for reference in "$app_ref" "$frontend_ref"; do
        actual_digest=$(containerd_image_digest "$reference") || return 1
        [ "$actual_digest" = "${reference#*@}" ] || return 1
    done

    while IFS=$'\t' read -r runtime_key runtime_image expected_digest unexpected; do
        [ -n "$runtime_key" ] && [ -n "$runtime_image" ] \
            && [ -n "$expected_digest" ] && [ -z "$unexpected" ] \
            || return 1
        actual_digest=$(containerd_image_digest "$runtime_image") || return 1
        [ "$actual_digest" = "$expected_digest" ] || return 1
    done <<< "$runtime_inventory"
}

if ! all_required_images_match; then
    info "외부 복구 묶음의 app/frontend/runtime 이미지를 k3s containerd에 가져옵니다."
    k3s_ctr images import "$images_archive"
fi

all_required_images_match \
    || die "복구 archive import 후 필수 app/frontend/runtime 이미지 digest를 확인하지 못했습니다."

# 기존 백업에 tag@digest만 있어도 CRI가 사용하는 tag 없는 이름을 복원한다.
ensure_containerd_image_alias "$app_ref" "$app_cri_ref" "$APP_IMAGE_DIGEST"
ensure_containerd_image_alias "$frontend_ref" "$frontend_cri_ref" "$FRONTEND_IMAGE_DIGEST"
info "복구 release의 모든 이미지 digest를 확인했습니다: $IMAGE_TAG"
