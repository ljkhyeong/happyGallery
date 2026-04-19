# Repository Guidelines

## 세션 시작 규칙
- 시작 전 `HANDOFF.md` 확인
- 코드와 다르면 즉시 갱신

## Skill 사용 기준
- 공통 규칙은 이 문서, 도메인 세부 절차는 `happyGallery` skill 우선
- 애매하면 `happygallery-spring-backend`
- 최종 응답에 항상 `사용한 스킬:`

## 승인 및 질문 최소화 규칙
- 불필요한 진행 확인 질문 금지
- `ps`, `lsof`, `curl`, `git status`, `git log`, `ls`, `rg`, `sed` 같은 로컬 조회는 바로 실행
- 빌드, 테스트, 헬스 체크, 프로세스/포트 확인도 바로 실행
- 권한 상승이 필요하면 실패 재현 없이 바로 요청
- 원격 작업, PR merge, 워크스페이스 밖 쓰기만 승인 흐름
- 파괴적이거나 모호한 작업만 예외적으로 확인
- PR은 mergeable 확인 후 충돌 없으면 바로 머지
- 과한 재검증, 과한 상태 점검, 중복 조회 금지

## 프로젝트 구조 및 모듈 책임
- `bootstrap/`: `@SpringBootApplication`, `application*.yml`, Flyway, logback, 마스킹 layout
- `adapter-in-web/`: 컨트롤러, 필터, resolver, 웹 전용 properties
- `adapter-out-persistence/`: JPA repository, MyBatis mapper/adapter
- `adapter-out-external/`: 결제, 알림, OAuth, Redis 세션, HTTP pool
- `application/`: 유스케이스 인터페이스(`port.in`/`port.out`), service, batch, `java-test-fixtures` 기반 공용 test support
- `domain/`: 엔티티, 정책 enum, 도메인 예외, 핵심 규칙
- 의존 방향: `bootstrap → adapter-in-web/out-* → application → domain` (ArchUnit `LayerDependencyArchTest`로 강제)

## 빌드, 테스트, 개발 명령어
- 모든 명령은 저장소 루트 + Gradle Wrapper 기준
- 대표 명령:
  - `./gradlew build`
  - `./gradlew test`
  - `./gradlew :bootstrap:bootRun`
  - `./gradlew :application:useCaseTest`
  - `./gradlew :application:policyTest`
  - `docker compose up -d`
- Testcontainers 계열은 기본적으로 `./gradlew --no-daemon ...`
- Gradle JVM, 원격 GitHub/git, Docker, Playwright는 필요 시 바로 권한 상승 실행
- 기본 프로필은 `local`, 헬스 체크는 `http://localhost:8080/actuator/health`

## 코딩 스타일 및 테스트 기준
- Java 21, Gradle toolchain, `com.personal.happygallery.<layer>.<feature>` 패키지 구조 유지
- 클래스 `UpperCamelCase`, 메서드/필드 `lowerCamelCase`, DTO는 `Request`/`Response`
- 컨트롤러는 검증/변환, 흐름은 서비스, 정책은 도메인에 둔다
- 리팩토링할 때는 수정 대상 주변만 보지 말고 `rg`로 동일/유사 패턴이 코드베이스에 더 있는지 먼저 확인한다.
- 동일한 리팩토링 이유가 성립하는 중복 패턴은 한 번에 같이 정리한다. 의도적으로 남겨야 하면 왜 남겼는지 답변이나 문서에 명시한다.
- import로 충분한 타입은 FQCN(`@org...`, `@jakarta...`)으로 본문에 직접 쓰지 않는다. 어노테이션도 일반 import를 사용하고, FQCN 표기는 이름 충돌을 피할 수 없는 예외적인 경우에만 허용한다.
- 테스트는 JUnit 5
- 정책 테스트는 `*PolicyTest`, 통합 흐름은 `@UseCaseIT` / `*UseCaseIT`
- 모든 테스트 메서드에 `@DisplayName` 한글 문장 사용
- 코드 수정 후에는 변경 범위 최소 테스트부터 실행
- 소스 코드를 수정하면 관련 문서를 항상 함께 갱신한다. 최소 `README.md`, `HANDOFF.md`, `docs/PRD`, `docs/ADR`, API 계약 문서 중 영향 범위를 확인하고 구현과 문서가 어긋난 상태로 두지 않는다.
- 구현 변경 시 관련 PRD, ADR, 운영 문서도 함께 반영

## DB 및 설정 변경 규칙
- DB 변경은 `bootstrap/src/main/resources/db/migration` 아래 Flyway만 사용
- 파일명은 `V<number>__description.sql`
- 환경별 설정은 `application-*.yml`, 공통은 `application.yml`
- 비밀값은 환경 변수로 주입, 저장소 하드코딩 금지

## 문서 작성 규칙
- 현재 활성 실행 계획은 저장소 루트 `plan.md`에만 기록
- 간단한 개선/리팩토링 아이디어는 저장소 루트 `simple-idea.md`에 기록
- `simple-idea.md`는 `As-Is | To-Be` 두 열 표로 한 줄씩 누적
- 완료된 임시 실행 계획은 `docs/1Pager`에 남기지 않음
- 오래 유지해야 하는 문서만 `docs/<Category>/0001_<topic>` 형식으로 관리
- 기준 스펙 문서는 `docs/PRD/0001_기준_스펙/spec.md`
- 요구사항 변경은 PRD와 구현 동시 갱신, 설계 변경은 ADR도 함께 검토
- 회고 문서는 `docs/Retrospective`에 기록
- 문서는 추상어·내부 은어보다 사용자 기준 표현과 구현 실체를 먼저 쓰고, 현재 상태와 변경 전후가 짧게 바로 읽히게 적는다.
- 주요 카테고리:
  - `docs/Idea`
  - `docs/1Pager`
  - `docs/PRD`
  - `docs/POC`
  - `docs/Retrospective`
  - `docs/ADR`

## 커밋 및 Pull Request 가이드
- 커밋 메시지는 `Feat:`, `Refactor:`, `Fix:`, `Test:`, `Docs:`, `Chore:`
- 커밋 메시지 prefix 뒤 본문은 특별한 이유가 없으면 한글로, 변경 내용을 드러내는 구체적인 내용으로 작성
- 커밋은 한 변경 의도에 집중해 작게 유지
- 작업 브랜치는 `codex/work-*`
- 기본 흐름: `codex/work-*` → `codexReview` Draft PR → `codexReview` merge → `main` PR → `main` merge
- PR 제목/본문은 특별한 이유가 없으면 한글
- PR 본문에는 문제, 핵심 설계 판단, 실행한 테스트, 문서 반영 여부 포함
