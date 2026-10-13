#!/usr/bin/env bash
# security-update.rb가 고친 build.gradle로 main 대상 보안 PR을 만들고 운영 후보 CI를 실행한다.
# 운영 배포 실패 뒤와 매일 보안 검사가 함께 쓴다. 자동 병합·재배포는 하지 않는다.
set -euo pipefail

: "${GITHUB_SHA:?GITHUB_SHA가 필요합니다.}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY가 필요합니다.}"
summary=${GITHUB_STEP_SUMMARY:-/dev/stdout}

if [ "$(gh api "repos/$GITHUB_REPOSITORY/commits/main" --jq .sha)" != "$GITHUB_SHA" ]; then
    echo 'main이 변경돼 오래된 검사 결과의 PR 생성을 생략합니다.' >> "$summary"
    exit 0
fi
branch="codex/work-security-${GITHUB_SHA:0:12}"
if git ls-remote --exit-code --heads origin "$branch" >/dev/null; then
    echo '기존 보안 업데이트 브랜치의 검증을 다시 실행합니다.' >> "$summary"
else
    git switch -c "$branch"
    git config user.name 'github-actions[bot]'
    git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
    git add build.gradle
    git commit -m 'Fix: Trivy 탐지 의존성 보안 패치 업데이트'
    git push origin "$branch"
fi
pr=$(gh pr list --head "$branch" --base main --state all --json url --jq '.[0].url // empty')
if [ -z "$pr" ]; then
    pr=$(gh pr create --head "$branch" --base main --title 'Fix: Trivy 탐지 의존성 보안 패치 업데이트' --body-file /tmp/security-update-body.md)
fi
echo "보안 업데이트 PR: $pr" >> "$summary"
gh workflow run ci.yml --ref "$branch" -f production_candidate=true
