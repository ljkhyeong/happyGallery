#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 1 ] || die "사용법: $0 <출력 YAML 경로>"
output=$1

: "${PUBLIC_HOST:?PUBLIC_HOST가 필요합니다.}"
: "${ACME_EMAIL:?ACME_EMAIL이 필요합니다.}"
: "${APP_IMAGE:?APP_IMAGE가 필요합니다.}"
: "${FRONTEND_IMAGE:?FRONTEND_IMAGE가 필요합니다.}"
: "${APP_IMAGE_DIGEST:?APP_IMAGE_DIGEST가 필요합니다.}"
: "${FRONTEND_IMAGE_DIGEST:?FRONTEND_IMAGE_DIGEST가 필요합니다.}"
: "${IMAGE_TAG:?IMAGE_TAG가 필요합니다.}"

printf '%s' "$PUBLIC_HOST" | grep -Eq '^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$' \
    || die "PUBLIC_HOST는 소문자 DNS 이름이어야 합니다."
printf '%s' "$ACME_EMAIL" | grep -Eq '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' \
    || die "ACME_EMAIL 형식이 올바르지 않습니다."
printf '%s' "$APP_IMAGE" | grep -Eq '^[A-Za-z0-9./_:@-]+$' \
    || die "APP_IMAGE에 허용되지 않은 문자가 있습니다."
printf '%s' "$FRONTEND_IMAGE" | grep -Eq '^[A-Za-z0-9./_:@-]+$' \
    || die "FRONTEND_IMAGE에 허용되지 않은 문자가 있습니다."
printf '%s' "$APP_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "APP_IMAGE_DIGEST는 sha256 digest여야 합니다."
printf '%s' "$FRONTEND_IMAGE_DIGEST" | grep -Eq '^sha256:[a-f0-9]{64}$' \
    || die "FRONTEND_IMAGE_DIGEST는 sha256 digest여야 합니다."
printf '%s' "$IMAGE_TAG" | grep -Eq '^[A-Fa-f0-9]{12,40}$' \
    || die "IMAGE_TAG는 12~40자리 Git SHA여야 합니다."

app_image_ref="$APP_IMAGE@$APP_IMAGE_DIGEST"
frontend_image_ref="$FRONTEND_IMAGE@$FRONTEND_IMAGE_DIGEST"

require_command awk
mkdir -p "$(dirname -- "$output")"
tmp=$(mktemp "${TMPDIR:-/tmp}/happygallery-manifests.XXXXXX")
trap 'rm -f "$tmp" "$tmp.rendered"' EXIT HUP INT TERM

{
    kube kustomize "$BASE_DIR"
    printf '%s\n' '---'
    cat "$DEPLOY_DIR/cluster/cluster-issuer.yaml"
} > "$tmp"

awk \
    -v host="$PUBLIC_HOST" \
    -v email="$ACME_EMAIL" \
    -v app_image="$app_image_ref" \
    -v frontend_image="$frontend_image_ref" \
    -v image_tag="$IMAGE_TAG" '
    {
        gsub(/__PUBLIC_HOST__/, host)
        gsub(/__ACME_EMAIL__/, email)
        gsub(/__APP_IMAGE__/, app_image)
        gsub(/__FRONTEND_IMAGE__/, frontend_image)
        gsub(/__IMAGE_TAG__/, image_tag)
        print
    }
' "$tmp" > "$tmp.rendered"

if grep -Eq '__[A-Z0-9_]+__' "$tmp.rendered"; then
    die "치환되지 않은 manifest 토큰이 남아 있습니다."
fi

mv "$tmp.rendered" "$output"
info "manifest 렌더링 완료: $output"
