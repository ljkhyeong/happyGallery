---
name: happygallery-spring-backend
description: happyGallery의 공통 설정·Gradle·모듈 구조·여러 도메인에 걸친 백엔드 리팩토링에 사용한다. 도메인이 명확하면 해당 전용 스킬을 먼저 사용한다.
---

# happyGallery 공통 백엔드

## 판단 기준

- HTTP 형식 검증은 웹 DTO, 단일 엔티티의 불변 조건은 도메인, 인증·소유권·여러 엔티티 간 검증은 application에 둔다.
- 검증을 제거하려면 같은 조건과 오류 시점을 다른 코드가 보장하는지 확인한다. 잠금·쓰기·외부 호출을 막는 사전 검사와 DB 제약은 별도로 판단한다.
- 중복 검색은 클래스 접미사뿐 아니라 구현한 port, repository 상속, `validate*`·`require*` 메서드도 확인한다.
- 단순 전달 adapter는 Spring Data repository가 port를 직접 구현할 수 있다. 암호화·제약 오류 변환·잠금·여러 repository 조합이 있으면 adapter를 유지한다.
- 읽기 모델은 projection·fetch join·일괄 조회를 검토한다. map은 결과 연결·중복 합산이 필요할 때 사용한다.
- 새 helper·전략·factory를 만들기 전에 Java·Spring·기존 의존성의 기능을 확인한다. 한 번 쓰는 표현에 이름만 붙이는 추출은 피한다.
- 단일 설정값은 `@Validated`로 검증하고, 값 정규화·필드 간 관계는 별도로 처리한다. 자동 설정으로 bean을 대체할 때는 실제 context에서 선택되는 구현을 확인한다.
- 파일 전체 검색은 트랜잭션 밖에서 한다. 삭제 직전 짧은 트랜잭션에서 참조 잠금과 재조회를 수행하고, 최초 검색과 재조회에 같은 URI 파서를 사용한다.
- DB에 작업을 저장한 뒤 실행하는 `AFTER_COMMIT` 비동기 처리는 실행 거절을 기록하고 복구 배치가 회수하게 한다. 이미 커밋한 요청을 실행 거절 때문에 5xx로 바꾸지 않는다.
- 외부 호출 timeout executor는 `AbortPolicy`를 유지한다. `CallerRunsPolicy`로 요청 스레드에서 실행하지 않는다. 풀은 `BoundedExecutorFactory`와 Boot builder를 사용하고 호출부에는 `Executor`를 주입한다.

## 검증 선택

- 단위·adapter: 해당 모듈의 `test --tests "*대상클래스*"`.
- 정책: `./gradlew :application:policyTest --tests "*대상클래스*"`.
- DB·트랜잭션·Flyway: `./gradlew --no-daemon :application:useCaseTest --tests "*대상클래스*"`.
- HTTP 계약은 `api-contract`, schema는 `entity-migration-sync`, 시간은 `time-boundary-policy`, 상태 전이는 `domain-state-machine`을 함께 적용한다.
- 모듈 구조 변경은 영향받는 모듈 compile과 `LayerDependencyPolicyTest`를 실행한다. 전체 build는 여러 모듈의 연동 확인이 필요할 때 선택한다.
