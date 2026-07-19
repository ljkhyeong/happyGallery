#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BASE_DIR="$DEPLOY_DIR/base"
REPO_ROOT=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
NAMESPACE=${NAMESPACE:-happygallery}

die() {
    printf '오류: %s\n' "$*" >&2
    exit 1
}

info() {
    printf '[happygallery] %s\n' "$*"
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "필수 명령을 찾을 수 없습니다: $1"
}

kube() {
    if [ -n "${KUBECTL_BIN:-}" ]; then
        "$KUBECTL_BIN" "$@"
    elif command -v kubectl >/dev/null 2>&1; then
        kubectl "$@"
    elif command -v k3s >/dev/null 2>&1; then
        k3s kubectl "$@"
    else
        die "kubectl 또는 k3s를 찾을 수 없습니다."
    fi
}

k3s_ctr() {
    if [ -n "${K3S_BIN:-}" ]; then
        "$K3S_BIN" ctr "$@"
    elif [ "$(id -u)" -eq 0 ]; then
        k3s ctr "$@"
    else
        sudo -- k3s ctr "$@"
    fi
}

env_value() {
    key=$1
    file=$2
    awk -F= -v key="$key" '
        /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
        $1 == key {
            sub(/^[^=]*=/, "")
            print
            found = 1
        }
        END { if (!found) exit 1 }
    ' "$file"
}

require_env_value() {
    key=$1
    file=$2
    value=$(env_value "$key" "$file") || die "$file 에 $key 값이 없습니다."
    [ -n "$value" ] || die "$file 의 $key 값이 비어 있습니다."
    printf '%s' "$value"
}

validate_env_file() {
    file=$1
    [ -f "$file" ] || die "환경 파일을 찾을 수 없습니다: $file"
    awk '
        /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
        !/^[A-Z][A-Z0-9_]*=[^\r\n]*$/ {
            printf "잘못된 환경 파일 행(%d): %s\n", NR, $0 > "/dev/stderr"
            bad = 1
        }
        {
            split($0, parts, "=")
            if (parts[1] != "" && seen[parts[1]]++) {
                printf "중복된 환경 변수(%d): %s\n", NR, parts[1] > "/dev/stderr"
                bad = 1
            }
        }
        END { exit bad ? 1 : 0 }
    ' "$file" || die "환경 파일 형식이 올바르지 않습니다: $file"
}

require_private_file() {
    file=$1
    [ -f "$file" ] || die "파일을 찾을 수 없습니다: $file"
    exposed=$(find "$file" -prune \( \
        -perm -001 -o -perm -002 -o -perm -004 -o \
        -perm -010 -o -perm -020 -o -perm -040 \) -print 2>/dev/null || true)
    [ -z "$exposed" ] || die "파일 권한을 600으로 제한한 뒤 다시 실행하세요: $file"
}

containerd_has_image() {
    image=$1
    normalized=$image
    case "$image" in
        */*) ;;
        *) normalized="docker.io/library/$image" ;;
    esac
    images=$(k3s_ctr images list -q)
    printf '%s\n' "$images" | grep -Fx "$image" >/dev/null \
        || printf '%s\n' "$images" | grep -Fx "$normalized" >/dev/null
}

containerd_image_digest() {
    image=$1
    k3s_ctr images list | awk -v image="$image" '
        NR > 1 && $1 == image {
            print $3
            found = 1
            exit
        }
        END { if (!found) exit 1 }
    '
}

wait_for_no_pods() {
    namespace=$1
    selector=$2
    max_attempts=${3:-120}
    attempt=0

    while :; do
        pods=$(kube -n "$namespace" get pods -l "$selector" -o name) \
            || die "Pod 목록을 조회할 수 없습니다: namespace=$namespace selector=$selector"
        [ -n "$pods" ] || break
        attempt=$((attempt + 1))
        [ "$attempt" -lt "$max_attempts" ] \
            || die "Pod 종료 대기 시간이 초과됐습니다: namespace=$namespace selector=$selector"
        sleep 1
    done
}

sha256_file() {
    file=$1
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$file" | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$file" | awk '{print $1}'
    else
        die "sha256sum 또는 shasum을 찾을 수 없습니다."
    fi
}

verify_checksum() {
    file=$1
    checksum_file=$file.sha256
    [ -f "$checksum_file" ] || die "체크섬 파일을 찾을 수 없습니다: $checksum_file"
    expected=$(awk 'NR == 1 { print $1 }' "$checksum_file")
    actual=$(sha256_file "$file")
    [ "$expected" = "$actual" ] || die "백업 체크섬이 일치하지 않습니다: $file"
}

base64_value() {
    printf '%s' "$1" | base64 | tr -d '\n'
}
