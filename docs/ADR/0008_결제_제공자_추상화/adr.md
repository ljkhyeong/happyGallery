# ADR-0008 결제 인터페이스 추상화 (§6.1)

- **날짜**: 2026-02-25
- **상태**: 결정됨

---

## 컨텍스트

§5.4 예약 취소에서 `Refund` 엔티티를 REQUESTED 상태로만 저장했다. 실제 PG 연동 없이도 환불 흐름을 테스트할 수 있어야 하고, 실패 시 레코드가 사라지지 않아야 한다.

---

## 결정 1 — 외부 결제 어댑터는 애플리케이션의 `PaymentPort`를 직접 구현한다

**선택**: `FakePaymentProvider`, `TossPaymentsProvider`, `ResilientPaymentProvider`가
`application/.../payment/port/out/PaymentPort.java`를 직접 구현한다.

**이유**: 애플리케이션 경계와 메서드가 완전히 같은 어댑터 전용 alias는 별도 계약을 제공하지 않는다.
구현체와 테스트가 `PaymentPort`를 직접 사용하면 결제 경계의 단일 소유자가 애플리케이션 포트로 분명해진다.

---

## 결정 2 — `FakePaymentProvider`는 항상 성공

**선택**: `FakePaymentProvider.refund()` → `RefundResult.success("FAKE-REFUND-{UUID}")`

**이유**: 개발 환경에서 기본값을 실패로 설정하면 매 취소마다 수동 재시도가 필요해 개발 흐름을 방해한다. 실패 시나리오는 테스트에서 `@MockitoBean`으로 주입한다.

---

## 결정 3 — 환불 요청을 먼저 저장하고 PG 결과를 상태별로 남긴다

**선택**: 취소 트랜잭션은 `RefundExecutionService`를 통해 `REQUESTED` 이력을 먼저 저장한다. 커밋 이후 PG를 호출하고 명시적 거절은 `FAILED`, 실행 전 일시 실패는 `RETRYABLE`, 반영 여부가 불명인 타임아웃·통신 단절은 `RECONCILIATION_REQUIRED`로 저장한다.

**이유**: 예약 취소 자체(`booking.cancel()`, 슬롯 반납)는 성공해야 한다. PG 환불 실패가 취소 트랜잭션을 롤백시키면 슬롯은 묶인 채 예약자는 취소할 수 없는 상태가 된다. 미완료 이력은 같은 멱등키로 자동 복구하고 운영자가 재처리할 수 있다.

**위험 포인트**: 예약은 취소됐으나 환불 금액이 아직 지급되지 않은 상태가 존재한다. 자동 복구와 `GET /admin/refunds/failed` 조치 필요 목록으로 추적한다. 상세 경계는 ADR-0018을 따른다.

---

## 결정 4 — 운영자 상태 조회·재시도 API

**선택**: `GET /api/v1/admin/refunds/{id}`로 단건 상태를 조회하고, `POST /api/v1/admin/refunds/{id}/retry`는 재처리 후 실제 저장 상태를 응답한다.

**이유**: 환불 시작 응답은 `REQUESTED`이므로 운영자가 PG 처리 결과를 별도로 확인할 수 있어야 한다. 조치 필요 레코드를 DB에서 직접 수정하지 않고 API로 재처리하면 시도 횟수와 결과 상태도 함께 추적할 수 있다.

**인증**: `/api/v1/admin/**`는 Spring Security 관리자 인증을 통과해야 한다.

---

## 결정 5 — 조치 필요 환불 응답은 원천 식별자를 함께 제공

**선택**: `GET /admin/refunds/failed` 응답에 `bookingId`, `orderId`, `passPurchaseId`, `paymentAttemptId`, 상태와 시도 횟수를 포함하고, 유형에 따라 사용하지 않는 식별자는 `null`로 반환한다.

**이유**: 예약 취소 환불과 주문 환불이 동일 `refunds` 테이블을 공유하므로,
운영자가 실패 건의 원천 엔터티를 즉시 식별할 수 있어야 한다.

**구현 포인트**: 조회는 `FAILED`, `RETRYABLE`, `RECONCILIATION_REQUIRED` 상태를 함께 반환한다.

---

## 결과

| 파일 | 역할 |
|------|------|
| `application/.../payment/port/out/PaymentPort.java` | 외부 결제 애플리케이션 포트 |
| `application/.../payment/port/out/RefundResult.java` | 환불 결과 VO (success/refundTransactionKey/failReason) |
| `adapter-out-external/.../payment/FakePaymentProvider.java` | 개발용 항상-성공 어댑터 |
| `domain/booking/Refund.java` | `markSucceeded()` / `markFailed()` 추가 |
| `application/.../booking/DefaultBookingCancelService.java` | Provider 호출, 실패 시 FAILED 저장 |
| `application/.../payment/DefaultRefundRetryService.java` | FAILED 재시도 서비스 |
| `adapter-in-web/.../admin/AdminRefundController.java` | `GET /admin/refunds/failed`, `GET /admin/refunds/{id}`, `POST /admin/refunds/{id}/retry` |
| `adapter-in-web/.../admin/dto/FailedRefundResponse.java` | `bookingId`/`orderId` nullable 응답 모델 |

## Update (2026-04-26)

Toss Payments 연동을 위해 결제 경계를 환불 전용에서 `prepare/confirm + refund`로 확장했다.

- `PaymentPort.confirm(paymentKey, orderId, amount, idempotencyKey)`와 `PaymentConfirmResult`를 추가했다.
- `POST /api/v1/payments/prepare`에서 서버가 `payment_attempt.order_id_external`과 `amount`를 확정한다.
- `POST /api/v1/payments/confirm`에서 PG confirm 성공 후 주문/예약/8회권 도메인 저장을 수행한다.
- confirm 요청 `paymentKey`는 `payment_attempt.payment_key`, PG 승인 응답의 `paymentKey`는 `payment_attempt.confirmed_payment_key`와 도메인 레코드의 `payment_key`에 저장해 환불 cancel 호출의 입력으로 사용한다.
- `refunds.payment_key`는 환불 재시도에 필요한 원결제 Toss `paymentKey`로 유지하고, 환불 성공 거래 식별자인 Toss cancel `transactionKey`는 `refunds.refund_transaction_key`에 별도로 저장한다.
- `FakePaymentProvider`는 local/test에서 confirm 성공 응답을 돌려주고, `TossPaymentsProvider`는 prod 프로필에서 Toss `/v1/payments/confirm`을 호출한다.

## Update (2026-07-12)

confirm의 트랜잭션 선점, Toss 멱등키, PG 승인 후 로컬 실패 보상 경계는
[ADR-0033](../0033_결제_confirm_트랜잭션과_보상_경계/adr.md)에서 관리한다.
