# happyGallery 작업 지침

## 진행과 소통

- 먼저 `HANDOFF.md`를 읽고, 요청에 필요한 코드·문서만 확인한다. 인계 내용이 코드와 다르면 바로 고친다.
- 요청과 대화에서 승인된 범위는 구현·검증·커밋까지 진행한다. 일반적인 구현 선택은 판단해서 처리하고, 결과를 좌우하는 요구사항이 빠졌거나 파괴적인 작업의 범위가 불명확할 때만 질문한다.
- 로컬 조회·수정·빌드·테스트는 바로 실행한다. 도구 권한이 필요하면 승인 절차를 사용하되, 이미 승인된 작업을 다시 물어보지 않는다. 별도 승인이 필요한 작업도 먼저 검토 가능한 결과를 준비한다.
- 사용자 요청은 스킬 지침보다 우선한다. 스킬 때문에 진행을 멈춰야 한다면 해당 `SKILL.md` 경로와 근거 문구를 밝힌다.
- 응답·문서·주석·PR 리뷰는 한글로 쓴다. 결과부터 설명하고, 실무 용어와 짧은 문장을 사용한다. 최종 응답에는 변경 결과, 검증 결과, `사용한 스킬:`을 적는다.

## 스킬

- 원본은 `.agents/skills/<이름>/SKILL.md`다. 공통 규칙은 이 문서에, 도메인 규칙은 해당 스킬에만 둔다.
- 변경을 주로 담당하는 스킬을 선택하고, API·DB·시간·상태 규칙도 바뀔 때만 해당 보조 스킬을 함께 읽는다. 공통 백엔드 변경은 `happygallery-spring-backend`, 화면은 `happygallery-frontend-flows`, 문서는 `happygallery-documentation-flows`를 사용한다.
- 스킬에는 적용 범위, 코드에서 놓치기 쉬운 규칙, 필요한 검증만 적는다. 구현 현황·전체 경로 목록·완료 이력은 복제하지 않는다.
- `CLAUDE.md`도 이 문서와 같은 스킬 원본을 참조한다. `.claude/skills`는 `../.agents/skills`를 가리키는 링크로 유지하고, 전역 경로에 프로젝트 스킬 사본을 추가하지 않는다.

## 코드와 데이터

- Java 25와 Gradle Wrapper를 사용한다. 패키지는 `com.personal.happygallery.<layer>.<feature>`, DTO 이름은 `Request`/`Response`를 따른다.
- 운영 코드 의존 방향은 `bootstrap → adapter-in-web/out-* → application → domain`이다. `LayerDependencyPolicyTest`가 이를 검사한다.
- `bootstrap`: 실행·설정·Flyway, `adapter-in-web`: HTTP 검증·변환·인증 필터, `application`: 유스케이스·트랜잭션, `domain`: 엔티티·정책, `adapter-out-persistence`: JPA·MyBatis, `adapter-out-external`: 외부 연동을 맡는다.
- 공용 fixture는 application/domain만 쓰면 `application/src/testFixtures`, 웹 DTO·영속성 의존이 있으면 `test-support/src/testFixtures`에 둔다. `test-support`는 테스트에서만 의존한다.
- 리팩토링 전 `rg`로 같은 패턴을 찾아 같은 이유로 바꿀 곳을 함께 정리한다. 남기는 예외는 이유를 설명한다.
- 이름 충돌이 없으면 FQCN 대신 import를 쓴다. adapter가 application port 메서드를 구현·재선언하면 `@Override`를 붙인다.
- DB 변경은 Flyway로 관리한다. SQL은 `bootstrap/src/main/resources/db/migration`, Java migration은 `bootstrap/src/main/java/com/personal/happygallery/bootstrap/migration`에 둔다. 버전은 두 경로를 확인해 정하고, 적용된 migration은 수정하지 않는다.
- 공통 설정은 `application.yml`, 환경별 설정은 `application-*.yml`에 둔다. 비밀값은 환경 변수로 주입한다.
- Controller·웹 DTO 계약을 바꾸면 `api-contract`를 적용해 REST Docs, OpenAPI, 생성 TypeScript client를 같은 커밋에서 갱신한다. 생성 파일은 직접 편집하지 않는다.

## 검증

- 변경을 확인할 수 있는 최소 검사부터 실행한다. 통과 후에는 추가 변경·실패·미해결 문제가 있을 때만 검사를 확대하거나 반복한다.
- 구현을 그대로 따라 쓰는 테스트나 문구 수정용 테스트는 추가하지 않는다. 테스트 전략은 ADR-0027을 따른다.
- JUnit 5를 사용하고 테스트 메서드에는 한글 `@DisplayName`을 붙인다. 정책은 `*PolicyTest`, 통합 흐름은 `@UseCaseIT` / `*UseCaseIT`로 구분한다.
- Gradle 명령은 저장소 루트에서 실행하고, Testcontainers 검사는 `--no-daemon`을 붙인다. 구체적인 대상은 해당 스킬에서 선택한다.

## 문서

- 제품 동작은 `docs/PRD/0001_기준_스펙/spec.md`, HTTP 계약은 `docs/PRD/0004_API_계약/spec.md`, 설계 결정은 `docs/ADR`, 실행·운영 안내는 `README.md`와 `deploy/`가 담당한다. 구현을 바꾸면 영향받는 문서를 함께 갱신한다.
- 활성 계획은 `plan.md`, 작은 개선안은 `simple-idea.md`의 `As-Is | To-Be` 표에 둔다. 오래 유지할 문서는 `docs/<Category>/0001_<topic>` 형식으로 관리하고, 완료된 임시 계획은 `docs/1Pager`에 남기지 않는다.
- `HANDOFF.md`에는 진행 중 상태·남은 행동·다음에 열 경로·세션에서 결정한 내용만 적는다. 기존 문서 내용은 경로로 연결한다. 진행 중 작업이 없으면 “진행 중 작업 없음”으로 비운다.

## 커밋과 PR

- 작업 브랜치는 `codex/work-*`를 사용한다. 완료한 변경을 의도별로 커밋하고, 다른 작업의 변경은 보존한다.
- 커밋은 `Feat:`, `Refactor:`, `Fix:`, `Test:`, `Docs:`, `Chore:` 뒤에 구체적인 한글 변경 내용을 적는다.
- 원격 푸시는 사용자가 요청한 경우에만 한다. 기본 PR 순서는 작업 브랜치 → `codexReview` Draft PR·병합 → `main` PR·병합이다.
- 병합 요청을 받으면 PR의 병합 가능 여부와 필요한 검사를 확인하고, 충돌이 없으면 재확인 질문 없이 병합한다.
- PR 제목·본문·리뷰는 한글로 작성한다. 본문에는 문제, 핵심 설계 판단, 실행한 검증, 문서 반영 여부를 적는다.
