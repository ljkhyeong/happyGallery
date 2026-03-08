# Codex Finish Automation

## 목적
- `claude-code`가 구현을 끝낸 뒤 `codex`가 동일한 후처리 순서를 빠뜨리지 않도록 고정한다.
- 테스트 로그 저장, 문서 반영 확인, 커밋, `codexReview` 대상 Draft PR 생성을 한 명령으로 처리한다.

## 자동화 범위
1. 지정한 테스트 명령 실행
2. `.context/test-logs/` 아래 로그 파일 저장
3. 테스트 실패 시 즉시 중단
4. 문서 반영 여부 확인
5. 현재 브랜치에 커밋
6. 원격 push
7. `codexReview` 대상 기존 PR 확인
8. PR이 없으면 Draft PR 생성, 있으면 재사용

테스트 실패 원인 분석과 코드 수정은 스크립트 범위가 아니다. 이 구간은 `codex`가 로그를 보고 직접 수정한 뒤 스크립트를 다시 실행한다.

## 전제 조건
- `git`, `gh`, `bash`가 설치돼 있어야 한다.
- `gh auth status`가 성공해야 한다.
- 작업 브랜치는 이미 생성돼 있어야 한다.
- 문서 반영 여부는 아래 둘 중 하나로 반드시 명시한다.
  - 문서를 실제로 수정했다면 `--doc <path>`
  - 문서 영향이 없다면 `--no-doc-update`

## 기본 사용법
```bash
scripts/codex_finalize.sh \
  --test-command "./gradlew :app:policyTest" \
  --commit-message "Feat: 자동 후처리 스크립트 추가" \
  --pr-title "자동 후처리 스크립트 추가" \
  --summary "codex 후처리 스크립트를 추가했습니다." \
  --summary "테스트 로그 저장과 Draft PR 생성 로직을 포함합니다." \
  --doc "docs/1Pager/0003_codex_finish_automation/guide.md"
```

## 권장 운영 절차
1. `claude-code`가 구현과 1차 수정까지 마친다.
2. `codex`가 변경 범위에 맞는 테스트 명령을 정한다.
3. 테스트 실패 시 로그를 보고 코드 수정 후 같은 명령을 다시 실행한다.
4. 테스트 성공 후 관련 문서를 반영한다.
5. `scripts/codex_finalize.sh`를 실행해 커밋과 PR 단계를 마무리한다.

## 옵션 요약
- `--test-command`: 실행할 테스트 명령
- `--commit-message`: 현재 브랜치 커밋 메시지
- `--pr-title`: 생성할 Draft PR 제목
- `--summary`: PR 본문 `변경 사항` 항목, 여러 번 사용 가능
- `--test-note`: PR 본문 `테스트 방법` 보조 항목, 여러 번 사용 가능
- `--doc`: 반영한 문서 경로, 여러 번 사용 가능
- `--no-doc-update`: 문서 영향 없음 명시
- `--tests-added`: PR 체크리스트의 `테스트 추가됨` 체크
- `--breaking-change`: Breaking change 존재 시 체크리스트 반영
- `--base-branch`: PR base 브랜치, 기본값 `codexReview`
- `--log-dir`: 테스트 로그 저장 경로, 기본값 `.context/test-logs`
- `--skip-pr`: 커밋과 push만 하고 PR 생성은 건너뜀

## 실패 처리 규칙
- 테스트 실패: 즉시 종료, 로그 파일 경로 출력
- 문서 미반영: `--doc`로 지정한 파일이 없거나 변경되지 않았으면 종료
- base 브랜치 없음: `origin/codexReview`가 없으면 종료
- 기존 PR 존재: 새 PR을 만들지 않고 기존 PR URL만 출력
