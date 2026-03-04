# Repository Guidelines

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

기본 프로필은 `local`이며, 헬스 체크는 `http://localhost:8080/actuator/health`에서 확인한다.

## 코딩 스타일 및 네이밍 규칙
Java 21과 Gradle toolchain 설정을 따른다. 패키지는 `com.personal.happygallery.<layer>.<feature>` 구조를 유지한다. 들여쓰기는 공백 4칸을 사용하고, 클래스는 `UpperCamelCase`, 메서드와 필드는 `lowerCamelCase`를 사용한다. 엔티티와 값 객체는 단수형 이름을 사용한다. 컨트롤러는 요청 검증과 응답 변환에 집중하고, 비즈니스 흐름은 서비스로, 정책 판단은 도메인 객체나 정책 클래스로 분리한다. DTO는 `Request`, `Response` 접미사를 명확히 붙인다.

## 테스트 작성 기준
테스트 프레임워크는 JUnit 5다. 정책이나 경계 조건 검증은 `*PolicyTest`로 작성하고 `policy` 태그를 사용한다. Spring Boot 컨텍스트, Flyway, Testcontainers(MySQL)를 함께 검증하는 흐름은 `@UseCaseIT`와 `*UseCaseIT` 형식으로 작성한다. 동작을 바꾸면 관련 테스트를 같이 수정하는 것을 기본으로 한다. 빠른 규칙 검증은 정책 테스트에, 트랜잭션·연동·동시성 검증은 통합 테스트에 둔다.

코드를 수정한 뒤에는 관련 테스트를 반드시 실행한다. 실패하면 원인을 수정한 뒤 성공할 때까지 다시 검증한다. 테스트는 항상 변경 범위에 맞는 최소 단위부터 선택한다. 정책이나 도메인 규칙만 바뀌면 `./gradlew :app:policyTest`를 우선 실행하고, 유스케이스 흐름이나 DB·외부 연동 영향이 있으면 `./gradlew :app:useCaseTest`까지 실행한다. 전체 안정성 확인이 필요한 큰 변경에서만 `./gradlew test` 또는 `./gradlew build`를 사용한다.

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
최근 커밋 메시지는 `Feat:`, `Refactor:`, `Test:` 같은 타입 접두사 뒤에 짧은 설명을 붙이는 형식이다. 필요하면 `§12.1` 같은 스펙 참조를 추가한다. 커밋은 하나의 변경 의도에 집중해 작게 유지한다. 코드를 작성하거나 수정한 작업은 관련 테스트 검증까지 마친 뒤 Pull Request 생성까지 완료하는 것을 기본으로 한다. Pull Request는 항상 현재 브랜치에 반영된 변경 사항 기준으로 Draft PR을 먼저 생성한다. PR 제목은 변경 내용을 한 줄로 요약하고, 본문은 저장소의 PR 템플릿을 사용해 주요 변경 사항을 리스트로 작성한다. Pull Request에는 해결하려는 문제, 핵심 설계 판단, 실행한 테스트 명령, API 변경 시 예시 요청·응답이나 화면 변경 자료를 포함한다.
