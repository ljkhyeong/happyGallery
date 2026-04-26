# ADR-0008 결제 인터페이스 추상화 (§6.1)

- **날짜**: 2026-02-25
- **상태**: 결정됨

---

## 컨텍스트

§5.4 예약 취소에서 `Refund` 엔티티를 REQUESTED 상태로만 저장했다. 실제 PG 연동 없이도 환불 흐름을 테스트할 수 있어야 하고, 실패 시 레코드가 사라지지 않아야 한다.

---

## 결정 1 — `PaymentProvider`는 외부 결제 어댑터 모듈에 둔다

**선택**: `adapter-out-external/.../payment/PaymentProvider.java`

**이유**: 현재 구조에서는 결제 구현이 외부 연동 어댑터 모듈에 있고, 애플리케이션은 `PaymentPort`를 통해 사용한다. `PaymentProvider`는 외부 결제 구현을 묶는 어댑터 쪽 타입으로 두는 편이 현재 구조와 맞다.

---

## 결정 2 — `FakePaymentProvider`는 항상 성공

**선택**: `FakePaymentProvider.refund()` → `RefundResult.success("FAKE-REFUND-{UUID}")`

**이유**: 개발 환경에서 기본값을 실패로 설정하면 매 취소마다 수동 재시도가 필요해 개발 흐름을 방해한다. 실패 시나리오는 테스트에서 `@MockitoBean`으로 주입한다.

---

## 결정 3 — 환불 실패 시 `FAILED`로 저장하고 예외를 삼킨다

**선택**: `DefaultBookingCancelService`에서 `paymentProvider.refund()` 호출 후 실패/예외 시 `refund.markFailed(reason)`으로 상태 업데이트 후 저장한다. 예외를 밖으로 전파하지 않는다.

**이유**: 예약 취소 자체(`booking.cancel()`, 슬롯 반납)는 성공해야 한다. PG 환불 실패가 취소 트랜잭션을 롤백시키면 슬롯은 묶인 채 예약자는 취소할 수 없는 상태가 된다. 환불 실패는 FAILED 레코드로 남기고 운영자가 재시도한다.

**위험 포인트**: 예약은 취소됐으나 환불 금액이 실제로 지급되지 않은 상태가 존재. 운영자가 `GET /admin/refunds/failed`를 주기적으로 확인하거나 알림 배치(§배치 구현 시)를 추가해야 한다.

---

## 결정 4 — 운영자 재시도 API: `POST /admin/refunds/{id}/retry`

**선택**: `DefaultRefundRetryService.retry(refundId)` + `AdminRefundController`

**이유**: FAILED 레코드를 DB에서 직접 수정하는 것은 감사 추적을 남기지 않는다. API를 통해 재시도하면 성공/실패 상태가 다시 기록되어 추적 가능.

**현재 제약**: `/admin/**` 인증 미적용 (§11 이후 적용 예정, ADR-0007과 동일).

---

## 결정 5 — 환불 실패 목록 응답은 `bookingId`/`orderId`를 함께 제공

**선택**: `GET /admin/refunds/failed` 응답에 `bookingId`, `orderId`를 모두 포함하고,
유형에 따라 사용하지 않는 값은 `null`로 반환한다.

**이유**: 예약 취소 환불과 주문 환불이 동일 `refunds` 테이블을 공유하므로,
운영자가 실패 건의 원천 엔터티를 즉시 식별할 수 있어야 한다.

**구현 포인트**:
- 조회 쿼리는 `booking` 연관이 없는 주문 환불도 누락되지 않도록 `LEFT JOIN FETCH`를 사용한다.
- named parameter 쿼리는 `@Param`으로 바인딩을 명시해 런타임 바인딩 오류를 방지한다.

---

## 결과

| 파일 | 역할 |
|------|------|
| `adapter-out-external/.../payment/PaymentProvider.java` | 외부 결제 어댑터 인터페이스 |
| `application/.../payment/port/out/RefundResult.java` | 환불 결과 VO (success/pgRef/failReason) |
| `adapter-out-external/.../payment/FakePaymentProvider.java` | 개발용 항상-성공 어댑터 |
| `domain/booking/Refund.java` | `markSucceeded()` / `markFailed()` 추가 |
| `application/.../booking/DefaultBookingCancelService.java` | Provider 호출, 실패 시 FAILED 저장 |
| `application/.../payment/DefaultRefundRetryService.java` | FAILED 재시도 서비스 |
| `adapter-in-web/.../admin/AdminRefundController.java` | `GET /admin/refunds/failed`, `POST /admin/refunds/{id}/retry` |
| `adapter-in-web/.../admin/dto/FailedRefundResponse.java` | `bookingId`/`orderId` nullable 응답 모델 |

## Update (2026-04-26)

Toss Payments 연동을 위해 결제 경계를 환불 전용에서 `prepare/confirm + refund`로 확장했다.

- `PaymentPort.confirm(paymentKey, orderId, amount)`와 `PaymentConfirmResult`를 추가했다.
- `POST /api/v1/payments/prepare`에서 서버가 `payment_attempt.order_id_external`과 `amount`를 확정한다.
- `POST /api/v1/payments/confirm`에서 PG confirm 성공 후 주문/예약/8회권 도메인 저장을 수행한다.
- confirm 성공 시 PG 원결제 참조값을 `payment_attempt.pg_ref`와 도메인 레코드의 `payment_key`에 저장해 환불 cancel 호출의 입력으로 사용한다.
- `FakePaymentProvider`는 local/test에서 confirm 성공 응답을 돌려주고, `TossPaymentsProvider`는 prod 프로필에서 Toss `/v1/payments/confirm`을 호출한다.
