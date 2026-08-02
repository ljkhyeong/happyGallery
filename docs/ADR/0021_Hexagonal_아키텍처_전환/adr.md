# ADR-0021: 현재 저장소는 포트/어댑터 구조를 기준으로 유지한다

**날짜**: 2026-03-15  
**상태**: Accepted
**갱신**: 2026-08-02

---

## 왜 이 문서가 필요한가

주문, 예약, 결제, 알림, 인증 기능이 커지면서 웹 진입점, DB 접근, 외부 연동이 한 방향으로만 정리되지 않으면 수정 범위가 빠르게 커진다.

특히 아래 문제가 반복됐다.

- 컨트롤러나 배치가 바로 저장소나 외부 구현을 알게 된다.
- 유스케이스 경계가 코드에서 바로 드러나지 않는다.
- 외부 연동 교체나 세션 저장소 변경 때 영향 범위가 넓어진다.
- 테스트가 기술 구현에 쉽게 묶인다.

한 번에 전체 구조를 다시 쓰는 방식은 운영 중인 서비스에 부담이 크다. 그래서 현재 저장소는 6개 운영 코드 모듈과 포트/어댑터 구조를 기준으로 유지한다.

---

## 결정

### 1. 운영 코드는 6개 모듈로 나눈다

- `bootstrap`: 앱 시작점, 환경 설정, Flyway, 로깅
- `adapter-in-web`: HTTP API, 필터, 요청/응답 처리
- `adapter-out-persistence`: JPA, MyBatis, DB 접근
- `adapter-out-external`: 결제, 알림, OAuth, Redis 세션, 외부 HTTP
- `application`: 유스케이스, 업무 로직, 배치, 포트 정의
- `domain`: 핵심 도메인 모델과 규칙

의존 방향은 `bootstrap -> adapter-in-web/out-* -> application -> domain` 으로 고정한다.

통합 테스트에서 웹 DTO와 영속성 repository를 함께 사용해야 하는 fixture는 보조 모듈
`test-support`의 test fixtures variant에 둔다. 이 모듈은 테스트 classpath에서만 소비하며,
운영 코드 6개 모듈의 의존 방향이나 `bootstrap` 실행 산출물에는 포함하지 않는다.

Gradle은 `java-library` 기준으로 구성한다. 공개 포트와 DTO 시그니처에 드러나는 하위 모듈·라이브러리는
`api`로 선언하고, 서비스 구현·어댑터 구현에서만 쓰는 Spring/Jackson/Redis/JPA 의존성은 `implementation`으로 둔다.

### 2. `application`이 유스케이스와 포트를 정의한다

- 외부에서 호출하는 진입점은 `...UseCase`
- 외부 구현이 채워 넣는 경계는 `...Port`

예시:

- inbound: `ApproveOrderUseCase`, `GuestClaimUseCase`
- outbound: `OrderReaderPort`, `PaymentPort`, `NotificationPort`

### 3. 웹과 배치는 유스케이스만 호출한다

- 컨트롤러는 요청 검증과 변환만 담당한다.
- 배치와 수동 트리거도 `UseCase`를 호출한다.
- 웹 모듈이 DB 어댑터나 외부 연동 구현을 직접 알지 않게 유지한다.

### 4. 모든 서비스에 인터페이스를 만들지는 않는다

인터페이스는 경계가 분명한 경우에만 만든다.

- 컨트롤러, 배치, 수동 트리거가 호출하는 유스케이스
- DB, 결제, 알림, 세션, 외부 API 같은 교체 가능한 경계

다음은 구현 클래스로 둔다.

- feature 내부 전용 조립 로직
- 하나의 유스케이스 안에서만 쓰는 계산/검증 도우미

### 5. 이름은 역할이 보이게 적는다

- 유스케이스 구현체: `Default*`
- 영속성 어댑터: `Jpa*Adapter`, `MyBatis*Adapter`, `*Repository`
- 외부 연동 어댑터: `*Sender`, `*PaymentProvider`, `*SessionStore`

`Impl` 접미사는 기본 규칙으로 쓰지 않는다.

### 6. 모듈 경계는 테스트로 고정한다

`LayerDependencyPolicyTest`가 아래 규칙을 검증한다.

- `domain`은 상위 계층을 참조하지 않는다.
- `application`은 어댑터와 `bootstrap`을 참조하지 않는다.
- `adapter-in-web`은 `adapter-out-*`를 직접 참조하지 않는다.
- `adapter-out-persistence`와 `adapter-out-external`은 서로 직접 의존하지 않는다.

### 7. Spring Data 저장소와 저장 포트 구현을 분리한다

- `JpaRepository` 인터페이스는 Spring Data 쿼리와 엔티티 저장 기술만 소유한다.
- `save(Entity)`를 선언하는 application 출력 포트는 `Jpa*PersistenceAdapter`가 명시적으로 구현하고
  해당 Spring Data 저장소에 위임한다.
- application 포트의 `save(Entity)`와 Spring Data의 `save<S extends Entity>(S)`를 한 인터페이스에서
  함께 상속하지 않는다. 타입 소거 뒤 동일 시그니처가 되어 unchecked bridge가 생성되기 때문이다.
- 저장 메서드가 없는 조회 전용 포트는 쿼리 선언과 시그니처가 일치하면 Spring Data 저장소가 직접
  구현할 수 있다. 경고 원인이 없는 조회 경계까지 형식적으로 adapter를 추가하지 않는다.
- 서로 다른 포트에 타입 소거 후 같은 시그니처가 있지만 의미나 반환형이 다르면 하나의 adapter로
  합치지 않는다. 각 포트의 저장소 의미를 보존하도록 adapter를 나눈다.

---

## 결과

### 장점

- 웹, 배치, DB, 외부 연동의 책임이 분명해진다.
- 유스케이스 경계가 코드에서 바로 보인다.
- 외부 연동이나 저장소 구현을 바꿀 때 영향 범위를 줄일 수 있다.
- 테스트에서 경계를 대체하기 쉬워진다.

### 단점

- 타입과 클래스 수가 늘어난다.
- 경계가 불필요한 곳까지 나누면 보일러플레이트만 늘 수 있다.

### 대응

- 인터페이스는 경계가 있는 경우에만 만든다.
- 이름 변경보다 의존 방향 정리를 우선한다.

---

## 구현 반영

- `settings.gradle`의 6개 운영 코드 모듈과 테스트 보조 `test-support` 모듈
- `build.gradle`의 `java-library` 공통 적용과 모듈별 `api`/`implementation` 의존성 분리
- `application/**/port/in`, `application/**/port/out`
- `adapter-in-web/**`, `adapter-out-persistence/**`, `adapter-out-external/**`
- `adapter-out-persistence/**/Jpa*PersistenceAdapter.java`
- `application/src/test/java/com/personal/happygallery/policy/LayerDependencyPolicyTest.java`
