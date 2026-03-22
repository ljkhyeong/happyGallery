# ADR-0021: 기존 app/domain/infra 구조 위에서 단계적으로 헥사고날 구조를 도입한다

**날짜**: 2026-03-15  
**상태**: Accepted

---

## 컨텍스트

현재 `happyGallery`는 `app / domain / infra / common` 모듈로 나뉘어 있다.
레이어 책임도 어느 정도 분리돼 있지만, 애플리케이션 서비스가 Spring Data repository나 외부 연동 구현을 직접 아는 구간이 아직 많다.

그 결과:

- `app`이 `infra` 기술 세부사항에 직접 묶이는 구간이 존재한다.
- controller / batch / scheduler가 호출하는 유스케이스 경계가 명시적으로 드러나지 않는 경우가 있다.
- 외부 연동 교체, 세션 저장소 변경, 모니터링 방식 전환 시 영향 범위가 넓어진다.
- 테스트에서도 “기술 구현을 모른 채 유스케이스만 검증”하기가 어려운 구간이 남는다.

현재 구조는 이미 운영 중이고, 기능 흐름과 통합 테스트도 충분히 쌓여 있다.
그래서 전체 구조를 한 번에 다시 쓰는 방식으로 헥사고날 구조를 강제하는 것은 위험이 크다.

따라서 전면 재작성 대신, 기존 모듈 구조를 유지한 채 `port / adapter`를 단계적으로 도입한다.

---

## 결정 사항

### 1. 기존 모듈 구조는 유지하고, 내부 경계를 헥사고날 방식으로 강화한다

- `domain`: 순수 비즈니스 규칙과 상태 전이
- `app`: 유스케이스 orchestration + `port` 정의
- `infra`: DB, 외부 연동, 세션, 메시징, 모니터링 등 구현체
- `common`: 공통 에러, 시간, 유틸

즉, 모듈을 다시 나누지 않고 `app` 안에 `port/in`, `port/out`를 두는 방식으로 바꾼다.

### 2. inbound port는 `...UseCase`, outbound port는 `...Port`로 명명한다

- inbound port:
  - `CreateGuestBookingUseCase`
  - `ApproveOrderUseCase`
  - `LoginCustomerUseCase`
- outbound port:
  - `PaymentPort`
  - `NotificationPort`
  - `CustomerSessionPort`
  - `OrderReaderPort`
  - `OrderStorePort`

### 3. 모든 service를 인터페이스화하지 않는다

다음 경우에만 인터페이스를 만든다.

- controller / batch / scheduler / manual trigger가 호출하는 명시적 유스케이스 경계
- persistence, 결제, 알림, 세션, 외부 API 등 교체 가능성이 있는 outbound 경계

다음은 구현 클래스로 둘 수 있다.

- feature 내부 전용 조립 도우미
- 하나의 유스케이스 안에서만 쓰는 조립/검증 도우미
- domain policy를 보조하는 내부 계산 서비스

즉, `서비스 = 무조건 interface + 구현체` 규칙은 쓰지 않는다.

### 4. 구현체 접미사 `Impl`은 사용하지 않고 `Default` 접두사를 사용한다

구현체 네이밍 규칙:

- 새로 분리하거나 rename하는 유스케이스/애플리케이션 서비스 구현체:
  - `DefaultCreateGuestBookingUseCase`
  - `DefaultLoginCustomerUseCase`
  - `DefaultGuestClaimUseCase`
- 기술 adapter 구현체:
  - `JpaOrderPersistenceAdapter`
  - `JpaCustomerSessionAdapter`
  - `PgPaymentAdapter`
  - `FakeNotificationAdapter`

금지:

- `FooServiceImpl`
- `BarRepositoryImpl`

초기 도입 단계에서는 기존 서비스명을 한꺼번에 바꾸지 않는다.
우선순위는 이름 변경보다 의존 방향 정리다. 이미 널리 쓰이는 서비스는 아래처럼 기존 이름을 유지한 채 포트를 구현할 수 있다.

- `CustomerAuthService implements CustomerAuthUseCase`
- `GuestClaimService implements GuestClaimUseCase`

이후 해당 구현체를 별도 use case 클래스로 분리하거나 rename할 때 `Default*` 규칙을 적용한다.

### 5. Spring Data `JpaRepository` 자체를 port로 보지 않는다

`JpaRepository`는 기술 인터페이스다.
헥사고날 전환에서 필요한 것은 애플리케이션 관점의 포트다.

따라서 다음처럼 유스케이스 관점으로 포트를 나눈다.

- `UserReaderPort`
- `UserStorePort`
- `GuestClaimQueryPort`
- `OrderReaderPort`
- `OrderStorePort`

즉, `app`은 `infra.*Repository`가 아니라 `app.port.out.*`를 의존해야 한다.

### 6. 전환 순서는 외부 연동 -> 저장소 분리 -> inbound 유스케이스 명시화 순으로 잡는다

우선순위:

1. 결제 / 알림 / 세션 저장소 같은 외부 경계 추출
2. `customer auth + guest claim` 도메인에서 저장소 분리를 먼저 적용
3. controller / batch / scheduler가 `UseCase` 인터페이스를 호출하도록 정리
4. order / booking / pass / product 도메인으로 확장

이 순서를 택하는 이유:

- 외부 연동은 교체 가능성이 높고 어디가 경계인지도 분명하다.
- persistence 전체를 먼저 건드리면 JPA / 테스트 영향 범위가 너무 넓다.
- `customer auth + guest claim`은 최근 member store 전환과 직접 맞물려 구조 개선 효과가 즉시 보인다.

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 외부 연동 교체, 세션 저장 전략 변경, 모니터링 방식 변경이 쉬워진다 |
| 장점 | controller / batch / scheduler가 같은 use case를 호출하게 되어 진입점 중복이 줄어든다 |
| 장점 | 테스트에서 기술 구현 대신 포트를 대체해 유스케이스 검증이 쉬워진다 |
| 장점 | `app`과 `infra`의 의존 방향이 더 명확해진다 |
| 단점 | 포트/어댑터/구현체가 늘어 보일 수 있어 보일러플레이트가 증가한다 |
| 단점 | 잘못 적용하면 `interface + Default + adapter`만 늘고 실익이 없는 구조가 될 수 있다 |
| 대응 | 모든 service를 인터페이스화하지 않고, 경계가 있는 의존성에만 port를 도입한다 |

---

## 구현 반영

- 기준 플랜: `plan.md`의 `Hexagonal Architecture Transition` 섹션
- 패키지 규칙:
  - `app/.../port/in`
  - `app/.../port/out`
  - `app/.../usecase` 또는 feature 패키지 안의 `Default*`
  - `infra/.../adapter`, `infra/.../persistence`, `infra/.../external`
- 네이밍 규칙:
  - inbound port: `...UseCase`
  - outbound port: `...Port`
  - 구현체: `Default*`
  - 기술 adapter: `Jpa*Adapter`, `Pg*Adapter`, `Fake*Adapter`

초기 적용 권장 범위:

- `customer auth`
- `guest claim`
- `customer/admin session`
- `payment`
- `notification`

### 2차 확산 (Track 5 완료)

product, notification, payment, booking, order 도메인으로 확장:

- **product**: `ProductReaderPort`(확장) + `ProductStorePort` + `InventoryReaderPort` + `InventoryStorePort` 도입, `ProductPersistencePortAdapter`/`InventoryPersistencePortAdapter`로 3개 서비스의 infra 직접 의존 제거
- **notification**: `NotificationLogStorePort` + `NotificationLogPersistencePortAdapter`로 `NotificationService`의 마지막 infra 의존 제거
- **payment**: `RefundExecutionService`/`RefundRetryService`/`RefundPort`를 `app.booking`에서 `app.payment`로 옮겨 주문과 예약이 같은 환불 호출 경로를 쓰게 한다
- **booking**: `BookingCreationSupport` 추출 — guest/member 예약 생성의 공통 orchestration (슬롯검증/락/8회권차감/예약금검증/저장+이력+알림)
- **order**: `OrderCreationService.createMemberOrder()` 추가 — 컨트롤러의 가격 조회 책임을 서비스 레이어로 이동
- **admin query**: `AdminBookingQueryService`/`AdminOrderQueryService` 도입 — 컨트롤러가 infra를 직접 보지 않게 하고, User batch fetch로 예약자/주문자 정보를 합쳐 만든다
