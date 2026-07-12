# ADR-0018: 환불 요청 커밋 이후 PG 호출 분리

**날짜**: 2026-03-06  
**상태**: Accepted

---

## 컨텍스트

주문 거절/자동환불/예약 취소 흐름에서 환불 호출이 실패하면 `refunds`에 `FAILED` 이력을 남겨
운영자가 재시도할 수 있어야 한다.

외부 PG 환불 호출은 네트워크 지연과 타임아웃 가능성이 있으므로 예약/주문/8회권 상태 변경을 수행하는
부모 트랜잭션 안에서 실행하면 안 된다. `NOT_SUPPORTED`로 트랜잭션을 suspend하는 방식도 부모
트랜잭션의 DB 커넥션을 계속 점유한 상태에서 PG 응답을 기다릴 수 있다.

---

## 결정 사항

- 환불 실행/이력 저장 로직을 `RefundExecutionService`로 분리한다.
- 환불 요청 레코드 생성은 `Propagation.MANDATORY`로 호출 유스케이스의 부모 트랜잭션에 참여한다.
  - `requestOrderRefund(orderId, amount, paymentKey)`
  - `requestBookingRefund(bookingId, amount)`
  - `requestPassRefund(passPurchaseId, amount, paymentKey)`
  - `requestPaymentAttemptRefund(paymentAttemptId, amount, paymentKey)`
- `RefundExecutionService`는 환불 요청을 저장한 뒤 `RefundExecutionRequestedEvent`를 발행한다.
- `RefundExecutionEventListener`는 `@TransactionalEventListener(AFTER_COMMIT)`과
  `@Async("refundExecutor")`로 부모 트랜잭션 커밋 뒤 PG 환불 실행을 시작한다.
- `RefundDispatcher`는 `Propagation.NEVER`로 실행되어 PG 환불 API를 활성 DB 트랜잭션 밖에서만 호출한다.
- PG 환불 API 호출 전 입력 조회와 호출 후 결과 업데이트만 짧은 `REQUIRES_NEW` 트랜잭션으로 처리한다.
  단, 원결제 `paymentKey`가 없어 PG 호출 자체가 불가능한 경우에는 입력 조회 트랜잭션 안에서 즉시 `FAILED`로 저장한다.
- 부모 트랜잭션이 롤백되면 환불 요청 레코드와 PG 환불 호출도 발생하지 않는다.
- PG 호출 실패는 커밋된 환불 요청 레코드를 `FAILED`로 갱신해 운영자 재시도 대상으로 남긴다.
- 운영자 재시도는 `Propagation.NEVER` 경계에서 즉시 실행해 HTTP 응답 전에 성공 또는 재실패 상태를 확정한다.
- `Refund`는 `bookingId`/`orderId`/`passPurchaseId`/`paymentAttemptId` 중 하나를 id-only 참조로 저장한다. 환불 이력은 재시도·운영 추적용 레코드이며,
  예약, 주문, 8회권 객체를 탐색하거나 상태를 변경하지 않는다.
- 환불 생성 시 UUID 멱등키를 저장하고 최초 PG 호출과 모든 재시도에서 동일하게 사용한다.

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 외부 PG 호출 중 부모 트랜잭션과 그 커넥션을 점유하지 않는다 |
| 장점 | 부모 트랜잭션 롤백 시 로컬 상태와 맞지 않는 외부 환불 호출이 발생하지 않는다 |
| 장점 | PG 실패는 커밋된 환불 요청 이력에 `FAILED`로 남아 재시도 가능하다 |
| 장점 | 운영자 재시도 API(`/api/v1/admin/refunds/failed`, `/retry`) 신뢰성이 올라간다 |
| 단점 | 환불 요청 API 응답 시점에는 실제 PG 결과가 아직 `REQUESTED`일 수 있다 |
| 대응 | 고객/운영자 알림과 실패 목록은 PG 결과 업데이트 이후 상태를 기준으로 처리한다 |

---

## 구현 반영

- `application/payment/RefundExecutionService`는 환불 요청 저장과 실행 이벤트 발행을 담당
- `application/payment/RefundExecutionEventListener`는 커밋 후 `refundExecutor` 실행을 담당
- `application/payment/RefundDispatcher`는 트랜잭션 밖 PG 호출과 결과 반영 흐름을 담당
- `application/payment/RefundTransactionService`는 `REQUIRES_NEW`가 필요한 PG 실행 준비·재시도 검증·결과 업데이트를 어노테이션 트랜잭션으로 담당
- `paymentKey` 누락처럼 PG 호출 전 확정 가능한 실패는 `prepareRefundCall` 안에서 조회와 `FAILED` 저장을 한 트랜잭션으로 처리
- `OrderApprovalService#processRefund` → `RefundExecutionService` 위임
- `BookingCancelService` 예약금 환불 경로 → `RefundExecutionService` 위임
- `PassRefundService` 8회권 환불 경로 → `RefundExecutionService` 위임
- PG 승인 후 도메인 생성 실패 보상 → `RefundExecutionService` 위임
- 부모 롤백 시 PG 호출/환불 이력 미생성 보장 테스트 추가:
  - `RefundExecutionServiceUseCaseIT`
- 커밋 이후 별도 executor에서 PG 호출 실행 보장 테스트 추가:
  - `RefundExecutionServiceUseCaseIT`
