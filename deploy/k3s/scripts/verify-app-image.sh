#!/usr/bin/env sh

set -eu
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

require_command docker
image=${1:?검사할 백엔드 이미지 참조가 필요합니다.}

info "운영 사용자로 이미지의 실행 JAR를 읽을 수 있는지 검사합니다: $image"
docker run --rm --pull=never --network none --read-only \
    --cap-drop ALL --security-opt no-new-privileges \
    --entrypoint sh "$image" -ec '
        [ "$(id -u)" = 10001 ] && [ "$(id -g)" = 10001 ] || {
            echo "앱 이미지의 실행 사용자가 10001:10001이 아닙니다." >&2
            exit 1
        }
        [ -s /app/app.jar ] && [ -r /app/app.jar ] || {
            echo "앱 실행 JAR가 없거나 비어 있거나 읽기 권한이 없습니다." >&2
            exit 1
        }
        cat /app/app.jar >/dev/null
        echo "앱 이미지 JAR 읽기 검사 통과"
    '
