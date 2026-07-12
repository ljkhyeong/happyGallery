# ADR-0033: 결제 confirm 트랜잭션과 보상 경계

**날짜**: 2026-07-12  
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

Toss Payments는 모든 POST API에서 `Idempotency-Key` 헤더를 지원하며, 같은 API 키·주소·HTTP 메서드와
같은 멱등키 조합의 최초 응답을 15일간 재사용한다.

## 결정

### 1. confirm 오케스트레이터에서는 DB 트랜잭션을 열지 않는다

`DefaultPaymentConfirmService`는 짧은 트랜잭션 메서드와 외부 PG 호출을 순서대로 조정한다.
Toss confirm은 활성 DB 트랜잭션이 없는 상태에서 호출한다.

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
저장된 payload와 현재 인증 주체의 조합은 claim 단계에서 한 번 검증하며, 이후 fulfiller는 이 계약을 전제로 실행한다.

### 3. Toss 멱등키를 요청마다 고정한다

- confirm: prepare에서 생성한 무작위 UUID `orderId`를 `Idempotency-Key`로 사용한다.
- refund: `refunds.idempotency_key`에 환불 생성 시 UUID를 저장하고 최초 실행과 모든 재시도에서 재사용한다.
- 멱등키만 바꿔 같은 요청을 재시도하지 않는다.

공식 계약: [토스페이먼츠 인증 및 기타 헤더 설정](https://docs.tosspayments.com/reference/using-api/authorization)

### 4. PG 승인과 도메인 생성을 별도 트랜잭션으로 처리한다

1. `PENDING/RETRYABLE -> PROCESSING` 선점
2. DB 트랜잭션 밖에서 PG confirm
3. 최종 PG 실패면 별도 트랜잭션으로 `FAILED`, 일시 실패면 `RETRYABLE`
4. PG 성공 또는 amount=0이면 별도 트랜잭션으로 `APPROVED`
5. 새 트랜잭션에서 도메인 생성과 `CONFIRMED`를 함께 커밋

`APPROVED`가 남으면 PG 재호출 없이 도메인 생성을 재개할 수 있다.

### 5. PG 승인 후 로컬 실패는 기존 환불 재시도 경로로 보상한다

- `refunds.payment_attempt_id`로 보상 대상 결제 시도를 식별한다.
- 결제 시도 `COMPENSATION_REQUESTED`와 환불 `REQUESTED`를 한 트랜잭션으로 저장한다.
- 커밋 이후 `RefundExecutionEventListener`가 `refundExecutor`에서 `RefundDispatcher`를 호출하고,
  `Propagation.NEVER`가 보장하는 비트랜잭션 구간에서 PG cancel을 실행한다.
- 성공하면 결제 시도는 `COMPENSATED`, 실패하면 `COMPENSATION_FAILED`가 된다.
- 실패한 보상 환불은 기존 관리자 실패 환불 목록과 재시도 API를 그대로 사용한다.

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | PG 네트워크 대기 중 DB 트랜잭션과 커넥션을 점유하지 않는다 |
| 장점 | PG 실패 상태가 예외 롤백과 무관하게 유지된다 |
| 장점 | 동시 confirm은 한 요청만 PG 호출을 수행한다 |
| 장점 | 타임아웃 재시도가 같은 Toss 멱등키를 사용한다 |
| 장점 | PG 승인 후 로컬 실패가 durable한 보상 환불과 운영자 재시도 대상으로 남는다 |
| 단점 | confirm 상태와 보상 상태가 늘어나 운영 조회가 복잡해진다 |
| 단점 | 비회원 confirm 응답 커밋 후 네트워크 단절 시 1회성 원문 access token 재발급 문제는 별도 보완이 필요하다 |

## 구현 반영

- `PaymentConfirmTransactionService`
- `DefaultPaymentConfirmService`
- `PaymentAttempt`, `PaymentAttemptStatus`
- `RefundExecutionService`, `RefundTransactionService`, `Refund`
- `TossPaymentsProvider`
- `V41__harden_payment_confirm_boundary.sql`
