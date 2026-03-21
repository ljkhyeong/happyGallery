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
- `app/`: 진입점, 웹, 서비스, 배치, 통합 테스트
- `domain/`: 엔티티, 정책, 핵심 규칙
- `infra/`: 저장소, 결제, 알림 등 외부 연동
- `common/`: 공통 예외, 시간 유틸리티
- 레이어 책임 준수: 도메인 규칙은 `domain`, 외부 연동은 `infra`

## 빌드, 테스트, 개발 명령어
- 모든 명령은 저장소 루트 + Gradle Wrapper 기준
- 대표 명령:
  - `./gradlew build`
  - `./gradlew test`
  - `./gradlew :app:bootRun`
  - `./gradlew :app:useCaseTest`
  - `./gradlew :app:policyTest`
  - `docker compose up -d`
- Testcontainers 계열은 기본적으로 `./gradlew --no-daemon ...`
- Gradle JVM, 원격 GitHub/git, Docker, Playwright는 필요 시 바로 권한 상승 실행
- 기본 프로필은 `local`, 헬스 체크는 `http://localhost:8080/actuator/health`

## 코딩 스타일 및 테스트 기준
- Java 21, Gradle toolchain, `com.personal.happygallery.<layer>.<feature>` 패키지 구조 유지
- 클래스 `UpperCamelCase`, 메서드/필드 `lowerCamelCase`, DTO는 `Request`/`Response`
- 컨트롤러는 검증/변환, 흐름은 서비스, 정책은 도메인에 둔다
- 테스트는 JUnit 5
- 정책 테스트는 `*PolicyTest`, 통합 흐름은 `@UseCaseIT` / `*UseCaseIT`
- 모든 테스트 메서드에 `@DisplayName` 한글 문장 사용
- 코드 수정 후에는 변경 범위 최소 테스트부터 실행
- 소스 코드를 수정하면 관련 문서를 항상 함께 갱신한다. 최소 `README.md`, `HANDOFF.md`, `docs/PRD`, `docs/ADR`, API 계약 문서 중 영향 범위를 확인하고 구현과 문서가 어긋난 상태로 두지 않는다.
- 구현 변경 시 관련 PRD, ADR, 운영 문서도 함께 반영

## DB 및 설정 변경 규칙
- DB 변경은 `app/src/main/resources/db/migration` 아래 Flyway만 사용
- 파일명은 `V<number>__description.sql`
- 환경별 설정은 `application-*.yml`, 공통은 `application.yml`
- 비밀값은 환경 변수로 주입, 저장소 하드코딩 금지

## 문서 작성 규칙
- 현재 활성 실행 계획은 저장소 루트 `plan.md`에만 기록
- 간단한 개선/리팩토링 아이디어는 저장소 루트 `simple-idea.md`에 기록
- `simple-idea.md`는 `As-Is | To-Be` 두 열 표로 한 줄씩 누적
- 완료된 임시 실행 계획은 `docs/1Pager`에 남기지 않음
- 오래 유지해야 하는 문서만 `docs/<Category>/0001_<topic>` 형식으로 관리
- 기준 스펙 문서는 `docs/PRD/0001_spec/spec.md`
- 요구사항 변경은 PRD와 구현 동시 갱신, 설계 변경은 ADR도 함께 검토
- 회고 문서는 `docs/Retrospective`에 기록
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
