---
name: happygallery-github-flows
description: happyGallery의 원격 푸시·PR 작성·CI 실패 분석·PR 리뷰 코멘트 대응·병합을 진행할 때 사용한다. 코드 수정 자체는 변경 영역의 도메인 스킬을 함께 사용한다.
---

# happyGallery GitHub·CI

## PR 흐름

- 순서는 `AGENTS.md`의 작업 브랜치 → `codexReview` → `main`을 따른다. Dependabot 일반 갱신은 `codexReview`, GitHub 보안 갱신은 `main`을 대상으로 한다.
- `main` push는 `production.yml`을 실행하고, `CD_ENABLED`가 켜져 있으면 이미지 게시와 운영 rollout까지 진행한다. `main` PR 병합 전 migration 호환성과 롤백 조건을 본문에서 확인한다.
- 본문은 `.github/pull_request_template.md` 구조에 문제·핵심 설계 판단·실행한 검증·문서 반영을 채운다. 호환성 검토는 세션 형식·데이터 의미·전환 절차가 바뀔 때만 쓰고 파일 목록은 적지 않는다.
- 본문은 임시 파일에 실제 줄바꿈으로 작성해 `gh pr create --body-file`·`gh pr edit --body-file`로 전달한다. `codexReview` → `main` PR에는 포함한 작업 PR과 CI 실행 링크를 적는다.
- 병합 commit 제목은 `<유형>: <내용>을 검토 브랜치에 병합`·`<유형>: <내용>을 main에 병합`, 본문은 PR 제목으로 한다.

## CI 실패 분석

- `gh pr checks <PR>`로 실패 작업을 찾고 `gh run view <run-id> --log-failed`로 실패 구간만 읽는다. 실행 ID를 기록하고 대기는 `gh run watch <run-id>`로 한다.
- 진단 산출물(`backend-*-diagnostics`, `e2e-smoke-diagnostics`)은 `gh run download <run-id> -n <이름> -D output/ci/<run-id>`로 받는다.
- 원인을 코드 결함·테스트 순서나 시간 의존·CI 환경(서비스 기동·캐시·외부 다운로드)으로 나눈다. `gh run rerun <run-id> --failed`는 일시 장애 근거가 있을 때만 쓴다.

| CI 작업 | 로컬 재현 |
|---|---|
| Rolling Compatibility | `ruby deploy/k3s/scripts/ci-compatibility.rb` |
| Agent Feedback Checks | `actionlint`, `ruby tools/agent-feedback-test.rb`, `ruby tools/check-agent-skills.rb` |
| Backend Package & Module Checks | `./gradlew --no-daemon build -x :application:check -x :adapter-in-web:check` |
| Backend Tests (application-*) | 실패 클래스만 `--tests`로 실행한다. 그룹 전체는 `:application:check -PciTestGroup=<core\|commerce\|migration>` |
| Backend Tests (adapter-in-web) | 실패 클래스를 `:adapter-in-web:test` 또는 `restDocsTest`로 실행한다 |
| Frontend Build | `frontend`에서 실패한 단계만 `test:unit`·`lint`·`api:check`·`build`로 실행한다 |
| Browser Smoke E2E | 실패 spec만 `npx playwright test <spec>`로 실행한다. CI는 local 프로필 jar와 `RATE_LIMIT_ENABLED=false`를 사용한다 |
| Dependency Review·Container Security | 보고된 패키지·CVE와 수정 버전을 확인한다 |

## 리뷰 코멘트와 병합

- `gh pr view <PR> --comments`와 `gh api repos/{owner}/{repo}/pulls/<PR>/comments`로 미해결 코멘트를 모은다. 번호별로 요구 수정·타당성·영향 범위를 요약하고, 결과를 바꾸는 판단만 사용자에게 묻는다.
- 수정은 코멘트 의도별로 커밋하고 같은 원인의 다른 위치를 `rg`로 함께 고친다. 답글에는 수정 commit 또는 유지 이유를 적는다.
- 병합 전 `gh pr view <PR> --json mergeable,mergeStateStatus,statusCheckRollup`으로 충돌과 필수 검사를 확인한다. 검사가 실패·대기 중이면 병합하지 않고 상태를 알린다.
