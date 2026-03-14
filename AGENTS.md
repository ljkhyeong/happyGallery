# Repository Guidelines

## 세션 시작 규칙
컨텍스트 초기화 후 작업을 시작할 때는 항상 저장소 루트의 `HANDOFF.md`를 먼저 읽고 현재 브랜치, 최근 결정, 검증 상태, 남은 작업을 확인한다. `HANDOFF.md`가 실제 구현과 다르면 코드를 기준으로 문서를 즉시 갱신한다.

## Skill 사용 기준
저장소 전체에 항상 적용되는 전역 규칙은 이 문서에서 관리하고, 도메인별 작업 절차와 세부 테스트 분기는 설치된 `happyGallery` skill을 우선 따른다.

- 전역 규칙: 세션 시작, 모듈 책임, 테스트/문서/DB/브랜치 정책
- 도메인 규칙: 예약/이용권/주문/상품/결제/관리자/알림/배치/테스트 리팩토링 관련 `happyGallery` skill
- 여러 영역에 걸친 변경이거나 맞는 skill이 없으면 `happygallery-spring-backend`를 기준으로 진행한다.
- 작업 완료 후 최종 응답에는 항상 `사용한 스킬:` 항목을 포함한다.
- 실제로 사용한 skill이 있으면 skill 이름을 모두 적고, 사용하지 않았으면 `없음`으로 명시한다.

## 승인 및 질문 최소화 규칙
작업 중 사용자 확인이나 승인 요청은 기술적으로 꼭 필요한 경우에만 한다. 단순 진행 확인, 저위험 상태 조회, 습관적인 중간 확인 질문은 하지 않는다.

- 먼저 현재 워크트리와 이미 승인된 명령으로 해결 가능한지 확인하고, 가능하면 그 범위 안에서 끝낸다.
- 원격 조회, push, PR merge, 워크스페이스 밖 경로 쓰기처럼 실제로 escalation이 필요한 작업만 승인 흐름을 탄다.
- 이 저장소에서 반복적으로 샌드박스에 막힌 작업은 처음부터 권한 상승 실행을 기본으로 한다. 대표적으로 Gradle JVM 명령, `gh pr create/view/merge/ready`, 원격 `git fetch/push/pull`, Docker 컨테이너 제어와 `docker exec`, Playwright 브라우저 설치/실행, 워크스페이스 밖 경로 또는 별도 worktree 쓰기가 이에 해당한다.
- 위 범주의 작업은 샌드박스에서 먼저 실패를 재현해보거나, 실패 원인을 설명하기 위한 추가 질문을 하지 말고 바로 권한 상승으로 진행한다.
- 승인 요청이 필요하면 관련 작업을 가능한 한 한 번에 이어서 처리할 수 있도록 묶고, `status`, `log`, `add` 같은 보조 명령 때문에 승인 횟수를 늘리지 않는다.
- 사용자가 `바로`, `지금`, `곧바로`처럼 실행 의도를 명확히 지시한 작업은 요구사항이 이미 확정된 것으로 보고, 파괴적이거나 모호한 경우가 아니면 재확인 질문 없이 진행한다.
- PR 작업에서는 먼저 mergeable 여부를 확인하고, 충돌이 없으면 바로 머지한다. 충돌이 있을 때만 필요한 파일만 정리한다.
- 사용자가 명시적으로 요구하지 않은 전체 재검증, 과한 상태 점검, 중복 조회는 피하고 변경 범위에 맞는 최소 검증만 수행한다.
- 파괴적 작업, 되돌리기 어려운 작업, 요구사항 해석이 갈릴 수 있는 작업만 예외적으로 사용자에게 직접 확인한다.

## 프로젝트 구조 및 모듈 책임
`happyGallery`는 멀티 모듈 Gradle 프로젝트다. `app/`에는 Spring Boot 진입점, 컨트롤러, 애플리케이션 서비스, 배치 작업, 통합 테스트가 있다. `domain/`은 엔티티와 정책처럼 핵심 비즈니스 규칙을 둔다. `infra/`는 JPA 리포지토리, 결제, 알림 등 외부 연동 구현을 담당한다. `common/`은 공통 예외와 시간 유틸리티를 둔다. 기능을 추가할 때는 먼저 레이어 책임에 맞는 모듈을 고르고, `app`에서 도메인 규칙을 직접 구현하거나 `infra`에 업무 규칙을 넣지 않는다.

## 빌드, 테스트, 개발 명령어
모든 명령은 저장소 루트에서 Gradle Wrapper로 실행한다.

- `./gradlew build`: 전체 모듈 컴파일과 전체 테스트 실행
- `./gradlew test`: 산출물 생성 없이 전체 테스트만 실행
- `./gradlew :app:bootRun`: 로컬 API 실행
- `./gradlew :app:useCaseTest`: `@Tag("usecase")` 통합 테스트 실행
- `./gradlew :app:policyTest`: `policy` 태그가 붙은 정책 테스트 실행
- `docker compose up -d`: 로컬 MySQL 등 의존 서비스 실행

테스트 명령의 세부 선택은 관련 `happyGallery` skill을 우선 따른다. Testcontainers를 사용하는 테스트(`:app:useCaseTest`, `@UseCaseIT`, 특정 `--tests` 대상 실행 포함)는 Docker 탐지 실패를 피하기 위해 기본적으로 `./gradlew --no-daemon ...` 형태로 실행한다.
Codex 실행 환경에서는 Gradle JVM 명령이 샌드박스에서 `FileLockContentionHandler` 소켓 생성 제한에 자주 걸린다. 그래서 `./gradlew test`, `./gradlew :app:test --tests ...`, `./gradlew :app:policyTest`, `./gradlew --no-daemon :app:useCaseTest`, `./gradlew :app:bootRun` 같은 Gradle 테스트/실행 명령은 처음부터 권한 상승 실행을 기본으로 하고, 샌드박스에서 먼저 한 번 실패시킨 뒤 재시도하지 않는다.
같은 이유로 원격 GitHub 작업(`gh pr *`), 원격 git 동기화, Docker 제어, Playwright 브라우저 설치처럼 이 저장소에서 반복적으로 샌드박스에 막힌 작업도 처음부터 권한 상승 실행을 기본으로 한다.

기본 프로필은 `local`이며, 헬스 체크는 `http://localhost:8080/actuator/health`에서 확인한다.

## 코딩 스타일 및 네이밍 규칙
Java 21과 Gradle toolchain 설정을 따른다. 패키지는 `com.personal.happygallery.<layer>.<feature>` 구조를 유지한다. 들여쓰기는 공백 4칸을 사용하고, 클래스는 `UpperCamelCase`, 메서드와 필드는 `lowerCamelCase`를 사용한다. 엔티티와 값 객체는 단수형 이름을 사용한다. 컨트롤러는 요청 검증과 응답 변환에 집중하고, 비즈니스 흐름은 서비스로, 정책 판단은 도메인 객체나 정책 클래스로 분리한다. DTO는 `Request`, `Response` 접미사를 명확히 붙인다.

## 테스트 작성 기준
테스트 프레임워크는 JUnit 5다. 정책이나 경계 조건 검증은 `*PolicyTest`로 작성하고 `policy` 태그를 사용한다. Spring Boot 컨텍스트, Flyway, Testcontainers(MySQL)를 함께 검증하는 흐름은 `@UseCaseIT`와 `*UseCaseIT` 형식으로 작성한다. 동작을 바꾸면 관련 테스트를 같이 수정하는 것을 기본으로 한다. 빠른 규칙 검증은 정책 테스트에, 트랜잭션·연동·동시성 검증은 통합 테스트에 둔다.
모든 테스트 메서드에는 `@DisplayName`을 붙이고, 테스트 의도를 드러내는 한글 문장으로 작성한다.

코드를 수정한 뒤에는 관련 테스트를 반드시 실행한다. 실패하면 원인을 수정한 뒤 성공할 때까지 다시 검증한다. 테스트는 항상 변경 범위에 맞는 최소 단위부터 선택한다. 기본 분기는 `policyTest` → `useCaseTest` → `test/build` 순서로 생각하되, 도메인별 더 구체적인 분기와 `--tests` 범위 선택은 관련 `happyGallery` skill을 따른다. Testcontainers 기반 검증은 위 원칙과 별개로 `--no-daemon`을 유지한다.
코드 수정이 완료되면 구현과 함께 유지돼야 하는 문서도 항상 같이 갱신한다. 최소한 관련 PRD, ADR, 운영 가이드, 저장소 작업 규칙 문서 중 변경 영향이 있는 문서를 확인하고 반영 여부를 명시한다.

## DB 및 설정 변경 규칙
DB 스키마 변경은 `app/src/main/resources/db/migration` 아래 Flyway 스크립트로만 반영한다. 파일명은 기존처럼 `V6__description.sql` 형식을 따른다. 환경별 설정은 `app/src/main/resources/application-*.yml`에 두고, 기본 프로필과 공통 설정은 `application.yml`에서 관리한다. `ADMIN_API_KEY`, DB 접속 정보 등 비밀값은 환경 변수로 주입하고 저장소에 하드코딩하지 않는다.

## 문서 작성 규칙
문서는 `docs` 아래를 문서 성격별 카테고리로 나눠 관리한다. 새 문서는 `docs/<Category>/0001_<topic>` 형식의 번호 기반 폴더를 만든 뒤 추가한다.

현재 프로젝트의 기준 스펙 문서는 `docs/PRD/0001_spec/spec.md`다. 구현, 테스트, 상태값, API 계약, 시간 경계 조건은 이 문서를 우선 기준으로 따르고, 요구사항 변경 시 `spec.md`와 구현을 함께 갱신한다.

- `docs/Idea`: 아이디어 스케치, 문제 정의, 초기 방향 정리
- `docs/1Pager`: 이해관계자 공유용 한 장 요약 문서
- `docs/PRD`: 기능 요구사항, 정책, API/화면, 비기능 요구사항 상세
- `docs/POC`: 실험 가설, 검증 방법, 결과, 결론 기록
- `docs/ADR`: 기술적·제품적 의사결정과 근거 기록

설계 판단이 바뀌면 ADR을 먼저 검토하고, 요구사항이 바뀌면 PRD와 구현을 함께 갱신한다.

## 커밋 및 Pull Request 가이드
최근 커밋 메시지는 `Feat:`, `Refactor:`, `Test:` 같은 타입 접두사 뒤에 짧은 설명을 붙이는 형식이다. 필요하면 `§12.1` 같은 스펙 참조를 추가한다. 커밋 메시지 제목과 Pull Request 제목/본문은 특별한 이유가 없으면 한글로 작성한다. 커밋은 하나의 변경 의도에 집중해 작게 유지한다. 코드를 작성하거나 수정한 작업은 관련 테스트 검증까지 마친 뒤 Pull Request 생성까지 완료하는 것을 기본으로 한다. 작업은 `codex/work-*` 같은 작업 브랜치에서 시작하고, 먼저 `codexReview`를 대상으로 Draft PR을 올려 리뷰와 통합 확인을 진행한다. 작업 완료 후에는 `codexReview`로 머지하고, `codexReview`에서 문제가 없을 때만 `main` 대상으로 최종 PR을 생성해 머지한다. PR 제목은 변경 내용을 한 줄로 요약하고, 본문은 저장소의 PR 템플릿을 사용해 주요 변경 사항을 리스트로 작성한다. Pull Request에는 해결하려는 문제, 핵심 설계 판단, 실행한 테스트 명령, API 변경 시 예시 요청·응답이나 화면 변경 자료를 포함한다. 모든 PR은 머지 전에 변경이력을 반드시 확인하고, 변경 영향이 있는 문서를 추가하거나 수정한 뒤 반영 내용을 PR 본문에 명시한다.
