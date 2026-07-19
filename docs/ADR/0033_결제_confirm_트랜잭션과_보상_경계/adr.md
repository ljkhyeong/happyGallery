# ADR-0033: 결제 confirm 트랜잭션과 보상 경계

**날짜**: 2026-07-12  
**최종 갱신**: 2026-07-19
**상태**: Accepted

---

## 컨텍스트

기존 `DefaultPaymentConfirmService.confirm()`은 하나의 DB 트랜잭션 안에서 Toss confirm과
주문·예약·8회권 생성을 모두 실행했다. 이 구조에는 다음 문제가 있었다.

- 외부 PG 응답을 기다리는 동안 DB 트랜잭션과 커넥션을 점유한다.
- PG 실패 시 `PaymentAttempt`를 `FAILED`로 `saveAndFlush()`한 뒤 예외를 던져도 전체 트랜잭션이 롤백된다.
- PG 승인 후 로컬 도메인 생성이 실패하면 외부 결제만 승인된 상태가 남는다.
- 동시 confirm 요청이 같은 `PENDING` 시도를 중복 처리할 수 있다.
- Toss POST 요청에 멱등키가 없어 타임아웃 재시도가 안전하지 않다.
- prepare와 confirm 사이에 상품·클래스·8회권 가격이 바뀌면 PG 승인 금액과 저장 도메인 금액이 달라질 수 있다.

Toss Payments는 모든 POST API에서 `Idempotency-Key` 헤더를 지원하며, 같은 API 키·주소·HTTP 메서드와
같은 멱등키 조합의 최초 응답을 15일간 재사용한다.

## 결정

### 1. confirm 오케스트레이터에서는 DB 트랜잭션을 열지 않는다

`DefaultPaymentConfirmService`는 짧은 트랜잭션 메서드와 외부 PG 호출을 순서대로 조정한다.
유스케이스 진입점은 `Propagation.NEVER`로 기존 트랜잭션 안의 호출을 거부하고, Toss confirm은
활성 DB 트랜잭션이 없는 상태에서만 호출한다. 서킷 브레이커·타임아웃·외부 통신 오류는
`PaymentPort` 구현이 null이 아닌 `PaymentConfirmResult`로 표준화한다. 포트 계약을 벗어난
예외과 null 반환은 재시도 가능한 PG 실패로 숨기지 않고 계약 위반으로 전파한다.

### 2. 결제 시도 상태를 단계별로 저장한다

- `PENDING`: prepare 완료
- `PROCESSING`: confirm 실행권 선점
- `RETRYABLE`: 타임아웃·서킷 오픈 등 같은 멱등키로 재시도 가능한 실패
- `APPROVED`: PG 승인 또는 amount=0 내부 승인 완료, 도메인 생성 전
- `CONFIRMED`: 도메인 생성 완료
- `FAILED`: 최종 PG 거절 또는 amount=0 도메인 생성 실패
- `COMPENSATION_REQUESTED`: PG 승인 후 도메인 생성 실패로 보상 환불 요청
- `COMPENSATION_FAILED`: 보상 환불 실패, 운영자 재시도 필요
- `COMPENSATED`: 보상 환불 완료

`PaymentConfirmTransactionService`의 각 변경은 `REQUIRES_NEW`로 실행한다. confirm 선점 조회에는
비관적 쓰기 잠금을 사용한다. `PROCESSING`이 1분 이상 지속되면 같은 paymentKey 요청만 다시 선점할 수 있다.
선점마다 `payment_attempt.processing_token`에 새 UUID를 저장하고, PG 승인·실패 결과는 현재 토큰과
일치할 때만 반영한다. 따라서 제한 시간을 넘겨 새 요청이 재선점한 뒤 늦게 끝난 이전 요청은 현재 상태를
변경하거나 보상 환불을 시작하지 못한다.

`resolveConfirmationStep()`은 nullable 값과 boolean 조합 대신 `Completed`, `ReadyForFulfillment`,
`PgConfirmationRequired`, `ZeroAmountApprovalRequired` 중 하나를 반환한다. 이 단계 결정 시
`PaymentPayload.userId()`와 현재 `AuthContext.userId()`를 공통 비교한다. 각
`PaymentFulfiller.validateStoredPayload()`는 컨텍스트별 저장 payload 불변식을 검증하고, 주문·예약·8회권은 저장된
가격 스냅샷과 `PaymentAttempt.amount`까지 비교한다. fulfillment는
현재 인증 정보를 다시 받지 않고 검증된 저장 payload의 `userId`를 사용한다. 비회원 연락처의 입력 형태는
prepare가 저장 전에 검증하고, 실제 휴대폰 인증 코드 소비는 fulfillment의 `VerifiedGuestResolver`가 담당한다.
`CONFIRMED`에는 생성된 도메인 ID와 비회원 접근 토큰 암호문을 함께 저장한다. 같은 사용자·금액·paymentKey의
재호출은 PG와 fulfillment를 반복하지 않고 저장된 결과를 복호화해 같은 응답을 반환한다.

### 3. 도메인 생성 금액은 prepare 시점에 확정한다

- 공개 입력인 `OrderPayload`에는 `productId`, `qty`만 받는다.
- `OrderPreparer`는 상품을 ID 목록으로 한 번에 조회하고, 서버 상품가를 포함한 내부용 `PreparedOrderPayload`와 amount를 함께 만든다.
- 내부용 payload는 AES-GCM으로 암호화해 `payment_attempt.payload_enc`에 저장하고, confirm 단계 결정과 fulfillment에서만 복호화한다. V46은 기존 평문 JSON도 암호문으로 전환한다.
- confirm 단계 결정에서 항목 단가 합계와 `payment_attempt.amount`를 대조한 뒤 PG를 호출한다.
- `OrderFulfiller`는 상품을 다시 조회하지 않고 저장된 단가로 `OrderItemRequest`를 만든다.
- `BookingPreparer`는 예약금과 잔금을 `PreparedBookingPayload`에, `PassPreparer`는 총 가격을 `PreparedPassPayload`에 저장한다.
- 예약과 8회권 fulfillment도 현재 가격을 다시 계산하지 않고 저장된 스냅샷으로 생성한다.
- 변경 전 생성되어 서버 가격이 없는 미확정 결제 시도는 confirm하지 않고 새 prepare를 요구한다.

`OrderItemRequest`가 결제 입력 항목과 `Product`를 받는 팩토리는 두지 않는다. confirm 경로가 `Product`에
의존하면 현재 가격 재조회와 계층 간 결합이 다시 생기므로, 서버가 확정한 원시 값만 명시적으로 전달한다.

### 4. Toss 멱등키를 요청마다 고정한다

- confirm: prepare에서 생성한 무작위 UUID `orderId`를 `Idempotency-Key`로 사용한다.
- Toss 승인 응답의 `paymentKey`, `orderId`가 요청값과 다르면 성공으로 수용하지 않고 같은 멱등키 재시도 대상으로 남긴다.
- refund: `refunds.idempotency_key`에 환불 생성 시 UUID를 저장하고 최초 실행과 모든 재시도에서 재사용한다.
- 멱등키만 바꿔 같은 요청을 재시도하지 않는다.

공식 계약: [토스페이먼츠 인증 및 기타 헤더 설정](https://docs.tosspayments.com/reference/using-api/authorization)

### 5. PG 승인과 도메인 생성을 별도 트랜잭션으로 처리한다

1. `PENDING/RETRYABLE -> PROCESSING` 선점과 새 processing token 저장
2. DB 트랜잭션 밖에서 PG confirm
3. 최종 PG 실패면 별도 트랜잭션으로 `FAILED`, 일시 실패면 `RETRYABLE`
4. PG 성공 또는 amount=0이면 별도 트랜잭션으로 `APPROVED`
5. 새 트랜잭션에서 도메인 생성과 `CONFIRMED`를 함께 커밋

`APPROVED`가 남으면 PG 재호출 없이 도메인 생성을 재개할 수 있다.
`CONFIRMED` 재호출은 최종 결과를 그대로 반환하므로 성공 응답 유실과 브라우저 새로고침도 멱등하다.
재선점 뒤 이전 processing token으로 도착한 PG 결과는 저장하지 않는다. 이때 최신 상태가 `CONFIRMED`면
저장된 결과를 반환하고, `APPROVED`면 PG 재호출 없이 fulfillment를 이어간다. 최신 요청이 아직
`PROCESSING`이거나 그 밖의 상태면 이전 요청에서 PG를 다시 호출하지 않고 `PAYMENT_CONFIRM_IN_PROGRESS`로 종료한다.
도메인 주문·예약에는 접근 토큰 해시만 유지하고, 재응답에 필요한 비회원 원문 토큰은
`payment_attempt.fulfilled_access_token_enc`에 AES-GCM 암호문으로만 보존한다.

### 6. PG 승인 후 로컬 실패는 기존 환불 재시도 경로로 보상한다

- `refunds.payment_attempt_id`로 보상 대상 결제 시도를 식별한다.
- 결제 시도 `COMPENSATION_REQUESTED`와 환불 `REQUESTED`를 한 트랜잭션으로 저장한다.
- 커밋 이후 `RefundExecutionEventListener`가 `refundExecutor`에서 `RefundDispatcher`를 호출하고,
  `Propagation.NEVER`가 보장하는 비트랜잭션 구간에서 PG cancel을 실행한다.
- 성공하면 결제 시도는 `COMPENSATED`가 된다. PG의 명시적 최종 거절만 `COMPENSATION_FAILED`로 전이한다.
- 큐 거절·서킷 오픈·일시 오류와 타임아웃·통신 단절은 `COMPENSATION_REQUESTED`를 유지하고 환불 복구 배치가 같은 멱등키로 다시 확인한다.
- 조치가 필요한 보상 환불은 기존 관리자 환불 목록과 재처리 API를 그대로 사용한다.
- amount=0 내부 승인은 외부 결제가 없으므로 로컬 생성 실패를 `FAILED`로 기록하고 보상 환불을 만들지 않는다.
- 보상 요청 트랜잭션 자체가 실패하면 원래 로컬 실패 예외에 보상 실패를 suppressed 원인으로 보존하고
  ERROR 로그를 남긴다. 이때 보상 이력은 아직 durable하지 않으며, 결제 시도는 `PROCESSING` 또는 `APPROVED`에
  남아 같은 confirm 재호출에서 동일 멱등키로 다시 처리된다.

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | PG 네트워크 대기 중 DB 트랜잭션과 커넥션을 점유하지 않는다 |
| 장점 | PG 실패 상태가 예외 롤백과 무관하게 유지된다 |
| 장점 | 동시 confirm은 한 요청만 PG 호출을 수행한다 |
| 장점 | 성공 응답 유실 뒤 같은 confirm을 재호출해도 동일 도메인 ID와 비회원 접근 토큰을 반환한다 |
| 장점 | 타임아웃 재시도가 같은 Toss 멱등키를 사용한다 |
| 장점 | prepare 이후 상품·클래스·8회권 가격이 바뀌어도 PG 승인 금액과 저장 도메인 금액이 일치한다 |
| 장점 | 저장에 성공한 PG 승인 후 로컬 실패가 durable한 보상 환불과 운영자 재시도 대상으로 남는다 |
| 단점 | confirm 상태와 보상 상태가 늘어나 운영 조회가 복잡해진다 |
| 단점 | 비회원 confirm 재응답을 위해 결제 시도 보존 기간 동안 접근 토큰 암호문을 추가 관리한다 |

## 구현 반영

- `PaymentConfirmTransactionService`
- `DefaultPaymentConfirmService`
- `PaymentPreparer`, `PaymentFulfiller`, `OrderPreparer`, `OrderFulfiller`, `BookingFulfiller`, `PassFulfiller`
- `PaymentPayload.PreparedOrderPayload`, `PreparedBookingPayload`, `PreparedPassPayload`
- `ProductReaderPort`, `ProductRepository`, `CartUseCase`
- `PaymentAttempt`, `PaymentAttemptStatus`
- `RefundExecutionService`, `RefundTransactionService`, `Refund`
- `TossPaymentsProvider`
- `V41__harden_payment_confirm_boundary.sql`
- `V46__ProtectPlaintextPersonalData`
- `V49__persist_payment_confirm_result.sql`
- `V50__fence_payment_confirm_processing.sql`
