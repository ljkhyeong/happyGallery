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

# 백그라운드 호출의 $!가 중간 셸이 아닌 실제 명령을 가리키게 한다.
exec_kube() {
    if [ -n "${KUBECTL_BIN:-}" ]; then
        exec "$KUBECTL_BIN" "$@"
    elif command -v kubectl >/dev/null 2>&1; then
        exec kubectl "$@"
    elif command -v k3s >/dev/null 2>&1; then
        exec k3s kubectl "$@"
    else
        die "kubectl 또는 k3s를 찾을 수 없습니다."
    fi
}

kube() (
    exec_kube "$@"
)

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

# 상태 경로 기본값은 호출 시점에 계산한다. source만 하는 스크립트는 HOME이 없어도 실패하지 않는다.
release_state_dir() {
    printf '%s' "${HAPPYGALLERY_RELEASE_DIR:-$HOME/.local/state/happygallery/releases}"
}

restore_state_dir() {
    printf '%s' "${HAPPYGALLERY_RESTORE_STATE_DIR:-$(dirname -- "$(release_state_dir)")/restores}"
}

cd_backup_env_file() {
    printf '%s' "${CD_BACKUP_ENV:-/etc/happygallery/cd-backup.env}"
}

cd_backup_dir() {
    printf '%s' "${HAPPYGALLERY_CD_BACKUP_DIR:-$(release_state_dir)/../cd/backups}"
}

newline='
'

# printf | grep는 줄 단위로 비교하므로 줄바꿈이 섞인 값은 패턴과 무관하게 거절한다.
matches_pattern() {
    case "$2" in
        *"$newline"*) return 1 ;;
    esac
    printf '%s' "$2" | grep -Eq "$1"
}

is_sha256_digest() {
    matches_pattern '^sha256:[a-f0-9]{64}$' "$1"
}

# IMAGE_TAG는 12~40자리 Git SHA다.
is_image_tag() {
    matches_pattern '^[A-Fa-f0-9]{12,40}$' "$1"
}

is_recovery_metadata_name() {
    matches_pattern '^happygallery-[0-9]{8}T[0-9]{6}Z\.recovery\.env$' "$1"
}

normalize_image_reference() {
    image=$1
    case "$image" in
        *@*)
            image_name=${image%%@*}
            image_suffix=@${image#*@}
            ;;
        *)
            image_name=$image
            image_suffix=
            ;;
    esac

    case "$image_name" in
        */*)
            registry=${image_name%%/*}
            case "$registry" in
                *.*|*:*|localhost) normalized=$image_name ;;
                *) normalized="docker.io/$image_name" ;;
            esac
            ;;
        *)
            normalized="docker.io/library/$image_name"
            ;;
    esac

    printf '%s%s' "$normalized" "$image_suffix"
}

containerd_has_image() {
    image=$1
    normalized=$(normalize_image_reference "$image")
    images=$(k3s_ctr images list -q)
    printf '%s\n' "$images" | grep -Fx "$image" >/dev/null \
        || printf '%s\n' "$images" | grep -Fx "$normalized" >/dev/null
}

containerd_image_digest() {
    image=$1
    normalized=$(normalize_image_reference "$image")
    # 목록을 끝까지 읽어 pipefail 사용 시 상위 명령의 SIGPIPE 종료를 막는다.
    k3s_ctr images list | awk -v image="$image" -v normalized="$normalized" '
        NR > 1 && !found && ($1 == image || $1 == normalized) {
            digest = $3
            found = 1
        }
        END {
            if (!found) exit 1
            print digest
        }
    '
}

ensure_containerd_image_alias() (
    alias_source=$(normalize_image_reference "$1")
    alias_target=$(normalize_image_reference "$2")
    alias_expected=$3
    alias_source_digest=$(containerd_image_digest "$alias_source") \
        || die "이미지 별칭의 원본을 찾을 수 없습니다: $alias_source"
    [ "$alias_source_digest" = "$alias_expected" ] \
        || die "이미지 별칭 원본의 digest가 다릅니다: $alias_source"
    if containerd_has_image "$alias_target"; then
        alias_digest=$(containerd_image_digest "$alias_target")
        [ "$alias_digest" = "$alias_expected" ] \
            || die "기존 이미지 별칭의 digest가 다릅니다: $alias_target"
    else
        k3s_ctr images tag "$alias_source" "$alias_target" >/dev/null
        alias_digest=$(containerd_image_digest "$alias_target")
        [ "$alias_digest" = "$alias_expected" ] \
            || die "등록한 이미지 별칭의 digest가 다릅니다: $alias_target"
    fi
)

# app/frontend Deployment는 Deployment와 같은 이름의 컨테이너 하나만 둔다.
deployment_container_image() {
    kube -n "$NAMESPACE" get deployment "$1" \
        -o "jsonpath={.spec.template.spec.containers[?(@.name==\"$1\")].image}"
}

# 실행 중인 이미지 참조는 release의 tag@digest 또는 CRI가 쓰는 tag 없는 이름@digest다.
image_matches_release() {
    case "$1" in
        "$2@$3"|"${2%:*}@$3") ;;
        *) return 1 ;;
    esac
}

mysql_database_query() {
    kube -n "$NAMESPACE" exec -i mysql-0 -- sh -ec '
        exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
          --batch --raw --skip-column-names --binary-mode "$MYSQL_DATABASE" "$@"
    ' sh "$@"
}

# 마지막으로 성공한 Flyway migration version 한 값을 출력한다. 비어 있을 때의 처리는 호출부가 정한다.
mysql_flyway_schema_version() {
    mysql_database_query \
        --execute='SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1'
}

mysql_preflight() {
    case "$1" in
        backup|restore) ;;
        *) die "MySQL 사전 검사 용도는 backup 또는 restore여야 합니다." ;;
    esac
    kube -n "$NAMESPACE" exec mysql-0 -- sh -ec '
        command -v mysql >/dev/null || { echo "mysql 실행 파일이 없습니다." >&2; exit 127; }
        if [ "$1" = backup ]; then
            command -v mysqldump >/dev/null || { echo "mysqldump 실행 파일이 없습니다." >&2; exit 127; }
            mysqldump --version >/dev/null
        fi
    ' sh "$1" || die "MySQL 필수 도구 사전 검사에 실패했습니다."
    mysql_database_query --execute='SELECT 1' >/dev/null \
        || die "MySQL 데이터베이스 접속 사전 검사에 실패했습니다."
}

check_mysql_database() {
    mysql_check_sql=$(mysql_database_query <<'SQL'
SELECT CONCAT('CHECK TABLE `', REPLACE(TABLE_SCHEMA, '`', '``'), '`.`',
              REPLACE(TABLE_NAME, '`', '``'), '`;')
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
SQL
    ) || die "MySQL 검사 대상 테이블 목록을 조회하지 못했습니다."
    [ -n "$mysql_check_sql" ] || die "MySQL 검사 대상 테이블이 없습니다."
    mysql_check_count=$(printf '%s\n' "$mysql_check_sql" | awk 'END { print NR }')
    mysql_check_result=$(printf '%s\n' "$mysql_check_sql" | mysql_database_query) \
        || die "MySQL CHECK TABLE 실행에 실패했습니다."
    # CHECK TABLE은 오류 행을 반환해도 mysql 자체는 성공할 수 있다.
    printf '%s\n' "$mysql_check_result" | awk -F '\t' -v expected="$mysql_check_count" '
        NF != 4 || $2 != "check" {
            print "잘못된 CHECK TABLE 응답: " $0 > "/dev/stderr"
            failed = 1
            next
        }
        $3 == "status" {
            if (seen[$1]++ || ($4 != "OK" && $4 != "Table is already up to date")) {
                print "테이블 검사 실패: " $0 > "/dev/stderr"
                failed = 1
            }
            checked++
            next
        }
        $3 != "info" && $3 != "note" {
            print "테이블 검사 오류: " $0 > "/dev/stderr"
            failed = 1
        }
        END {
            if (checked != expected) {
                printf "테이블 검사 결과 누락: expected=%d actual=%d\n", expected, checked > "/dev/stderr"
                failed = 1
            }
            exit failed ? 1 : 0
        }
    ' || die "MySQL 테이블 무결성 검사를 통과하지 못했습니다."
    info "MySQL 테이블 무결성 검사 통과: $mysql_check_count 개"
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

sha256_stream() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 | awk '{print $1}'
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

verify_recovery_bundle_files() {
    bundle_metadata=$1
    [ -f "$bundle_metadata" ] || die "복구 메타데이터를 찾을 수 없습니다: $bundle_metadata"
    require_private_file "$bundle_metadata"
    verify_checksum "$bundle_metadata"
    validate_env_file "$bundle_metadata"

    bundle_database_name=$(require_env_value DATABASE_BACKUP "$bundle_metadata")
    bundle_media_name=$(require_env_value MEDIA_BACKUP "$bundle_metadata")
    bundle_image_tag=$(require_env_value IMAGE_TAG "$bundle_metadata")
    bundle_release_relative=$(require_env_value RELEASE_DIR "$bundle_metadata")
    bundle_app_digest=$(require_env_value APP_IMAGE_DIGEST "$bundle_metadata")
    bundle_frontend_digest=$(require_env_value FRONTEND_IMAGE_DIGEST "$bundle_metadata")

    case "$bundle_database_name" in
        */*|.|..) die "DATABASE_BACKUP은 복구 메타데이터와 같은 디렉터리의 파일명이어야 합니다." ;;
    esac
    case "$bundle_media_name" in
        */*|.|..) die "MEDIA_BACKUP은 복구 메타데이터와 같은 디렉터리의 파일명이어야 합니다." ;;
    esac
    is_image_tag "$bundle_image_tag" \
        || die "복구 메타데이터의 IMAGE_TAG 형식이 올바르지 않습니다."
    [ "$bundle_release_relative" = "releases/$bundle_image_tag" ] \
        || die "RELEASE_DIR은 IMAGE_TAG와 일치하는 releases/<IMAGE_TAG> 형식이어야 합니다."

    bundle_root=$(CDPATH= cd -- "$(dirname -- "$bundle_metadata")" 2>/dev/null && pwd) \
        || die "복구 묶음 디렉터리를 찾을 수 없습니다: $bundle_metadata"
    bundle_database="$bundle_root/$bundle_database_name"
    bundle_media="$bundle_root/$bundle_media_name"
    bundle_release="$bundle_root/$bundle_release_relative"
    bundle_release_metadata="$bundle_release/metadata.env"
    bundle_release_manifest="$bundle_release/manifests.yaml"
    bundle_runtime_metadata="$bundle_release/runtime-images.env"
    bundle_images_archive="$bundle_release/images.tar"

    for bundle_file in \
        "$bundle_database" "$bundle_media" \
        "$bundle_release_metadata" "$bundle_release_manifest" \
        "$bundle_runtime_metadata" "$bundle_images_archive"; do
        [ -f "$bundle_file" ] || die "복구 묶음 파일을 찾을 수 없습니다: $bundle_file"
        verify_checksum "$bundle_file"
    done
    validate_env_file "$bundle_release_metadata"
    validate_env_file "$bundle_runtime_metadata"

    [ "$(require_env_value IMAGE_TAG "$bundle_release_metadata")" = "$bundle_image_tag" ] \
        || die "복구 메타데이터와 release IMAGE_TAG가 다릅니다."
    [ "$(require_env_value APP_IMAGE_DIGEST "$bundle_release_metadata")" = "$bundle_app_digest" ] \
        || die "복구 메타데이터와 release app digest가 다릅니다."
    [ "$(require_env_value FRONTEND_IMAGE_DIGEST "$bundle_release_metadata")" = "$bundle_frontend_digest" ] \
        || die "복구 메타데이터와 release frontend digest가 다릅니다."
}

decode_base64() {
    if printf '' | base64 --decode >/dev/null 2>&1; then
        base64 --decode
    elif printf '' | base64 -d >/dev/null 2>&1; then
        base64 -d
    else
        base64 -D
    fi
}

base64_value() {
    printf '%s' "$1" | base64 | tr -d '\n'
}

app_secret_encoded() {
    kube -n "$NAMESPACE" get secret happygallery-app -o "jsonpath={.data.$1}"
}

# happygallery-app Secret 값을 디코드해 출력한다. 없는 key는 빈 값이고 조회 실패는 그대로 전파한다.
app_secret_value() {
    secret_encoded=$(app_secret_encoded "$1") || return
    printf '%s' "$secret_encoded" | decode_base64
}

# 디코드한 값을 변수에 담지 않고 그대로 해시해 끝의 줄바꿈·NUL도 fingerprint에 반영한다.
app_secret_fingerprint() {
    secret_encoded=$(app_secret_encoded "$1") || return
    printf '%s' "$secret_encoded" | decode_base64 | sha256_stream
}

app_key_rotation_phase() {
    secret_rotation_phase=$(kube -n "$NAMESPACE" get secret happygallery-app \
        -o 'jsonpath={.metadata.annotations.happygallery\.io/key-rotation-phase}' 2>/dev/null || true)
    printf '%s' "${secret_rotation_phase:-none}"
}

media_helper_pod_name() {
    printf '%s' "${MEDIA_HELPER_POD:-media-maintenance}"
}

ensure_media_pvc() {
    if kube -n "$NAMESPACE" get pvc app-media >/dev/null 2>&1; then
        return
    fi
    info "기존 클러스터의 첫 미디어 백업을 위해 app-media PVC를 먼저 생성합니다."
    kube -n "$NAMESPACE" apply -f "$BASE_DIR/app-media-pvc.yaml" >/dev/null
}

start_media_helper() {
    image=$1
    helper_pod=$(media_helper_pod_name)

    kube -n "$NAMESPACE" delete pod "$helper_pod" --ignore-not-found --wait=true >/dev/null
    kube -n "$NAMESPACE" apply -f - >/dev/null <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $helper_pod
  labels:
    app.kubernetes.io/name: media-maintenance
    app.kubernetes.io/part-of: happygallery
spec:
  restartPolicy: Never
  automountServiceAccountToken: false
  securityContext:
    runAsNonRoot: true
    runAsUser: 10001
    runAsGroup: 10001
    fsGroup: 10001
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: media-maintenance
      image: $image
      imagePullPolicy: Never
      command: ["sh", "-ec", "trap : TERM INT; sleep 3600 & wait"]
      resources:
        requests:
          cpu: 25m
          memory: 64Mi
        limits:
          cpu: 250m
          memory: 256Mi
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop: ["ALL"]
      volumeMounts:
        - name: media
          mountPath: /media
        - name: tmp
          mountPath: /tmp
  volumes:
    - name: media
      persistentVolumeClaim:
        claimName: app-media
    - name: tmp
      emptyDir:
        sizeLimit: 64Mi
EOF
    kube -n "$NAMESPACE" wait --for=condition=Ready "pod/$helper_pod" --timeout=2m >/dev/null
    kube -n "$NAMESPACE" exec "$helper_pod" -- tar --version >/dev/null
}

stop_media_helper() {
    helper_pod=$(media_helper_pod_name)
    kube -n "$NAMESPACE" delete pod "$helper_pod" --ignore-not-found --wait=true >/dev/null 2>&1 || true
}
