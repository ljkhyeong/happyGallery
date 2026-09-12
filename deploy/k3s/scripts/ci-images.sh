#!/usr/bin/env bash
set -Eeuo pipefail
. "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)/common.sh"
cd "$REPO_ROOT"

[[ ${GITHUB_SHA:-} =~ ^[a-f0-9]{40}$ ]] || die "40자리 GITHUB_SHA가 필요합니다."
[[ ${GITHUB_REPOSITORY:-} == ljkhyeong/happyGallery ]] || die "허용되지 않은 저장소입니다."
[[ ${GITHUB_REF:-} == refs/heads/main ]] || die "main에서만 운영 이미지를 게시합니다."
source_url=https://github.com/ljkhyeong/happyGallery
registry=ghcr.io/ljkhyeong

case "${1:-}" in
    build)
        [[ -n ${VITE_TOSS_CLIENT_KEY:-} && $VITE_TOSS_CLIENT_KEY != test_ck_ci ]] \
            || die "PRODUCTION_TOSS_CLIENT_KEY를 GitHub Actions 변수에 설정하세요."
        jar=bootstrap/build/libs/happygallery-app.jar
        [[ -s $jar ]] || die "같은 CI 실행에서 검증한 app JAR가 없습니다."
        docker build --platform linux/amd64 \
            --label "org.opencontainers.image.revision=$GITHUB_SHA" \
            --label "org.opencontainers.image.source=$source_url" \
            --build-arg "APP_JAR=$jar" -f deploy/k3s/images/Dockerfile.app \
            -t happygallery-app:production .
        "$SCRIPT_DIR/verify-app-image.sh" happygallery-app:production
        docker build --platform linux/amd64 \
            --label "org.opencontainers.image.revision=$GITHUB_SHA" \
            --label "org.opencontainers.image.source=$source_url" \
            --build-arg "VITE_TOSS_CLIENT_KEY=$VITE_TOSS_CLIENT_KEY" \
            --build-arg "VITE_SENTRY_DSN=${VITE_SENTRY_DSN:-}" \
            --build-arg VITE_SENTRY_ENVIRONMENT=production \
            --build-arg "VITE_SENTRY_RELEASE=happygallery@$GITHUB_SHA" \
            -f deploy/k3s/images/Dockerfile.frontend -t happygallery-frontend:production .
        ;;
    publish)
        : "${GHCR_TOKEN:?GHCR_TOKEN이 필요합니다.}"
        : "${GITHUB_OUTPUT:?GITHUB_OUTPUT이 필요합니다.}"
        : "${GITHUB_ACTOR:?GITHUB_ACTOR가 필요합니다.}"
        umask 077
        DOCKER_CONFIG=$(mktemp -d)
        export DOCKER_CONFIG
        trap 'rm -rf "$DOCKER_CONFIG"' EXIT
        printf '%s' "$GHCR_TOKEN" | docker login ghcr.io --username "$GITHUB_ACTOR" --password-stdin
        unset GHCR_TOKEN
        for component in app frontend; do
            image="$registry/happygallery-$component"
            docker tag "happygallery-$component:production" "$image:$GITHUB_SHA"
            docker push "$image:$GITHUB_SHA"
            digest=$(docker image inspect "$image:$GITHUB_SHA" --format '{{json .RepoDigests}}' \
                | ruby -rjson -e 'refs=JSON.parse(STDIN.read).select { |ref| ref.start_with?(ARGV[0]+"@") }; abort "registry digest가 모호합니다." unless refs.one?; puts refs.first.split("@",2).last' "$image")
            [[ $digest =~ ^sha256:[a-f0-9]{64}$ ]] || die "registry digest 형식이 올바르지 않습니다."
            printf '%s_digest=%s\n' "$component" "$digest" >> "$GITHUB_OUTPUT"
            printf '%s@%s\n' "$image" "$digest" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
        done
        ;;
    *) die "사용법: $0 <build|publish>" ;;
esac
