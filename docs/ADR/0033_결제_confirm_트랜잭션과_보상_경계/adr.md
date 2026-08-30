# ADR-0033: 결제 confirm 트랜잭션과 보상 경계

**날짜**: 2026-07-12  
**최종 갱신**: 2026-08-30
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
예외와 null 반환은 재시도 가능한 PG 실패로 숨기지 않고 계약 위반으로 전파한다.

### 2. 결제 시도 상태를 단계별로 저장한다

- `PENDING`: prepare 완료
- `PROCESSING`: confirm 실행권 선점
- `RETRYABLE`: 타임아웃·서킷 오픈 등 같은 멱등키로 재시도 가능한 실패
- `APPROVED`: PG 승인 또는 amount=0 내부 승인 완료, 도메인 생성 전
- `CONFIRMED`: 도메인 생성 완료
- `FAILED`: 최종 PG 거절 또는 amount=0 도메인 생성 실패
- `RECONCILIATION_REQUIRED`: Toss 응답 식별자가 요청과 다르거나 멱등 응답 안전 기간을 지나
  자동 재확인할 수 없어 수동 대사가 필요한 상태
- `COMPENSATION_REQUESTED`: PG 승인 후 도메인 생성 실패로 보상 환불 요청
- `COMPENSATION_FAILED`: 보상 환불 실패, 운영자 재시도 필요
- `COMPENSATED`: 보상 환불 완료

confirm 상태 변경은 트랜잭션 책임에 따라 세 개의 package-private 서비스로 분리하고 각 변경을
`REQUIRES_NEW`로 실행한다.

- `PaymentConfirmClaimTransactionService`: 실행권 선점, processing token fencing, PG 승인·실패 결과와 늦은 승인 화해
- `PaymentConfirmFulfillmentTransactionService`: 도메인 생성과 `CONFIRMED` 저장, fulfillment 실패의 보상 요청
- `PaymentConfirmRecoveryTransactionService`: 행 잠금 아래 복구 후보 재검증, 저장된 요청 복원과 대사 전환

저장 payload 복호화, 컨텍스트별 fulfiller 선택, 완료 결과 복원은 비트랜잭션 협력 객체인
`PaymentConfirmAttemptResolver`에서 공유한다. 따라서 트랜잭션 서비스 간 검증 구현을 중복하지 않으면서도
오케스트레이터가 각 `REQUIRES_NEW` 빈을 거쳐 실제 트랜잭션 경계를 통과한다.

confirm 선점 조회에는 비관적 쓰기 잠금을 사용한다. `PROCESSING`이 1분 이상 지속되면 같은 paymentKey 요청만 다시 선점할 수 있다.
선점마다 `payment_attempt.processing_token`에 새 UUID를 저장하고, 일반 PG 승인·실패 결과는 현재 토큰과
일치할 때만 반영한다. 재선점 뒤 늦게 도착한 실패는 버리지만, 외부에서 이미 성립한 PG 승인 성공은
유실하면 안 된다. 같은 orderId·금액·paymentKey 요청임을 다시 검증하고 잠금 안에서 최신 상태가
`PROCESSING/RETRYABLE/FAILED`면 `APPROVED`로 화해한다. 이 경로는 보상 환불을 바로 시작하지 않고 fulfillment를 재개한다.

`resolveConfirmationStep()`은 nullable 값과 boolean 조합 대신 `Completed`, `ReadyForFulfillment`,
`PgConfirmationRequired`, `ZeroAmountApprovalRequired` 중 하나를 반환한다. 이 단계 결정 시
저장 전용 `PreparedPaymentPayload.userId()`와 현재 `AuthContext.userId()`를 공통 비교한다. 각
`PaymentFulfiller.validateStoredPayload()`는 컨텍스트별 저장 payload 불변식을 검증하고, 주문·예약·8회권은 저장된
가격 스냅샷과 `PaymentAttempt.amount`까지 비교한다. fulfillment는
현재 인증 정보를 다시 받지 않고 검증된 저장 payload의 `userId`를 사용한다. 비회원 주문·예약은
비회원 prepare의 전화번호별 Redis 시도 제한은 DB 트랜잭션을 열기 전에 확인한다. 그 뒤 prepare
트랜잭션에서 휴대폰 인증 코드를 잠금 후 소비하고 `PaymentAttempt`와 함께 커밋하며,
`context + orderId + 정규화 전화번호 + nonce`에 HMAC 서명한 증거만 서버 확정 payload에 저장한다.
fulfillment의 `VerifiedGuestResolver`는 현재
`PaymentAttempt`와 전화번호에 대한 증거 서명을 확인하며 원 인증 코드를 다시 조회하거나 소비하지 않는다.
`CONFIRMED`에는 생성된 도메인 ID와 비회원 접근 토큰 암호문을 함께 저장한다. 같은 사용자·금액·paymentKey의
재호출은 PG와 fulfillment를 반복하지 않고 저장된 결과를 복호화해 같은 응답을 반환한다.

### 3. 도메인 생성 금액은 prepare 시점에 확정한다

- 공개 입력인 `OrderPayload`에는 `productId`, `productVariantId`, 직접입력 옵션, `qty`와 고객이 선택한 수령 방식, 배송 주문의 구조화된 배송지를 받는다.
- `OrderPreparer`는 같은 상품·SKU·직접입력 요청을 먼저 합산해 SKU별 1~99개 제한을 적용하고, `ACTIVE` 상품과 옵션 구성을 ID 목록으로 일괄 조회한다. 서버는 기본가·조합 추가금·직접입력 추가금, SKU·옵션·수령 방식·배송지 스냅샷을 포함한 내부용 `PreparedOrderPayload`와 amount를 함께 만들며, SKU별 재고 요구량을 합산해 확인하고 상품가와 총액은 웹 안전 정수 상한을 넘지 않게 한다.
- 공개 prepare 입력은 `PaymentPayload`의 `ORDER/BOOKING/PASS`만 허용한다. 서버 확정 스냅샷은 별도
  `PreparedPaymentPayload`의 `PREPARED_ORDER/PREPARED_BOOKING/PREPARED_PASS`로 분리한다. 내부 스냅샷은
  HTTP schema에 노출하지 않고, 공개 요청 타입은 fulfiller 계약에 전달하지 않는다. 저장 JSON의 기존 subtype
  이름은 유지해 암호화된 레코드와 키 회전 호환성을 보존한다.
- 내부용 payload는 AES-GCM으로 암호화해 `payment_attempt.payload_enc`에 저장하고, confirm 단계 결정과 fulfillment에서만 복호화한다. 비회원 공개 입력의 인증 코드 원문은 저장하지 않고 결제 귀속 HMAC 증거로 교체한다. V46은 기존 평문 JSON도 암호문으로 전환한다.
- confirm 단계 결정에서 항목 단가 합계와 `payment_attempt.amount`를 대조한 뒤 PG를 호출한다.
- `OrderFulfiller`는 상품을 다시 조회하지 않고 저장된 가격 구성과 옵션 스냅샷으로 `OrderItemRequest`를 만들며, 주문·SKU 재고 차감과 선택된 fulfillment를 같은 트랜잭션에 저장한다. 배송지는 별도 AES-GCM 암호문으로 `fulfillments.shipping_address_enc`에 보존한다.
- `BookingPreparer`는 예약금과 잔금을 `PreparedBookingPayload`에, `PassPreparer`는 총 가격을 `PreparedPassPayload`에 저장한다.
- 예약과 8회권 fulfillment도 현재 가격을 다시 계산하지 않고 저장된 스냅샷으로 생성한다.
- 변경 전 생성되어 서버 가격이 없는 미확정 결제 시도는 confirm하지 않고 새 prepare를 요구한다.

`OrderItemRequest`가 결제 입력 항목과 `Product`를 받는 팩토리는 두지 않는다. confirm 경로가 `Product`에
의존하면 현재 가격 재조회와 계층 간 결합이 다시 생기므로, 서버가 확정한 원시 값만 명시적으로 전달한다.

### 4. Toss 멱등키를 요청마다 고정한다

- 브라우저 기본 선택은 Toss `CARD` 통합 결제창이다. 네이버페이·카카오페이를 선택하면 같은 SDK에 `card.flowMode=DIRECT`,
  선택한 `card.easyPay` 코드(`NAVERPAY` 또는 `KAKAOPAY`), `windowTarget=self`를 전달한다. 두 간편결제는 공통 선택 UI와
  SDK 옵션 생성·약관 확인을 사용한다. 전용창의 토스 결제 약관 동의는 prepare 전에 확인하고, 결제수단 변경 시 초기화한다.
  선택값은 브라우저 진입 방식일 뿐 별도 PG나 서버 결제 상태를 추가하지 않으며, 기존 승인·부분환불·정산을 재사용한다. prepare payload의
  예약 결제수단은 PG 호출 전 표시용 스냅샷일 뿐이며, 승인·조회 응답의 `method`를 `confirmed_payment_method`에 저장해
  최종 예약 결제수단으로 사용한다.
- confirm: prepare에서 생성한 무작위 UUID `orderId`를 `Idempotency-Key`로 사용한다.
- 결제창 취소·실패의 화면 복귀 경로는 기존 고객 세션 귀속 `PaymentReturnHint.returnPath`에 보관한다. 실패 콜백의
  `orderId`나 외부 query를 복귀 주소로 사용하지 않는다. 로그인 복귀에서 사용하는 내부 주소 확인을 재사용하고,
  링크 표시와 클릭 시 고객 세션을 확인한다. prepare에서 받은 결제 ID도 같은 hint에 보관해 복귀 전에 승인 전 결제를 종료한다.
  종료 API는 confirm과 같은 행 잠금과 소유권 검증 아래 `PENDING -> CANCELED`, payload 제거와 혜택 예약 해제를 함께 커밋한다.
  `CANCELED` 재요청은 성공하며 나머지 상태는 변경 없이 거절한다. 실패 화면은 종료 실패 시 조회 자격을 보존하고 현재 상태를 표시한다.
  SDK 오류에도 같은 종료를 시도하며, 브라우저 자체 종료와 요청 실패는 기존 30분 만료 배치가 정리한다. prepare·confirm·환불은 자동 재요청하지 않는다.
  예약 시간·재고·약관 동의는 복원하지 않고 구매 화면에서 다시 확인한다. 성공 화면의 승인·대사·보상 환불 경로는 유지한다.
- Toss 승인 응답의 `paymentKey`, `orderId`가 요청값과 다르면 해당 응답을 결제 시도에 귀속할 수 없으므로
  현재 processing token 소유자만 즉시 `RECONCILIATION_REQUIRED`로 전이한다. 동일 응답을 자동 재시도하지 않는다.
- refund: `refunds.idempotency_key`에 환불 생성 시 UUID를 저장하고 최초 실행과 모든 재시도에서 재사용한다.
- 멱등키만 바꿔 같은 요청을 재시도하지 않는다.

공식 계약: [토스페이먼츠 인증 및 기타 헤더 설정](https://docs.tosspayments.com/reference/using-api/authorization)

자체창 계약: [토스페이먼츠 카드사 및 간편결제 자체창 연동](https://docs.tosspayments.com/guides/v2/payment-window/integration-direct)

### 5. PG 승인과 도메인 생성을 별도 트랜잭션으로 처리한다

1. `PENDING/RETRYABLE -> PROCESSING` 선점과 새 processing token 저장
2. DB 트랜잭션 밖에서 PG confirm
3. 최종 PG 실패면 별도 트랜잭션으로 `FAILED`, 일시 실패면 `RETRYABLE`
4. PG 성공 또는 amount=0이면 별도 트랜잭션으로 `APPROVED`
5. 새 트랜잭션에서 도메인 생성과 `CONFIRMED`를 함께 커밋

`APPROVED`가 남으면 PG 재호출 없이 도메인 생성을 재개할 수 있다.
`CONFIRMED` 재호출은 최종 결과를 그대로 반환하므로 성공 응답 유실과 브라우저 새로고침도 멱등하다.
재선점 뒤 이전 processing token으로 도착한 PG 실패는 저장하지 않는다. 이때 최신 상태가 `CONFIRMED`면
저장된 결과를 반환하고, `APPROVED`면 PG 재호출 없이 fulfillment를 이어간다. 최신 상태가
`PROCESSING`이면 `PAYMENT_CONFIRM_IN_PROGRESS`, `RETRYABLE`이면 `PAYMENT_CONFIRM_RETRYABLE`,
`FAILED`면 저장된 이유의 `PAYMENT_FAILED`, `RECONCILIATION_REQUIRED`면
`PAYMENT_RECONCILIATION_REQUIRED`, 보상·취소 상태면 `INVALID_INPUT`으로 종료하며 이전 요청에서 PG를 다시 호출하지 않는다.
반면 이전 processing token의 PG 성공은 외부 승인 사실이므로, 최신 로컬 실패보다 우선해
`APPROVED`로 화해하고 fulfillment를 이어간다.
도메인 주문·예약에는 접근 토큰 해시만 유지하고, 재응답에 필요한 비회원 원문 토큰은
`payment_attempt.fulfilled_access_token_enc`에 AES-GCM 암호문으로만 보존한다.

### 6. PG 승인 후 로컬 실패는 기존 환불 재시도 경로로 보상한다

- `refunds.payment_attempt_id`로 보상 대상 결제 시도를 식별한다.
- 결제 시도 `COMPENSATION_REQUESTED`와 환불 `REQUESTED`를 한 트랜잭션으로 저장한다.
- 커밋 이후 `RefundExecutionEventListener`가 `refundExecutor`에서 `RefundDispatcher`를 호출하고,
  `Propagation.NEVER`가 보장하는 비트랜잭션 구간에서 PG cancel을 실행한다.
- `refundExecutor` 포화 또는 종료 중 거절은 이미 커밋된 환불 요청을 실패로 되돌릴 수 없으므로
  메트릭과 경고만 남기고 호출자에게 전파하지 않는다. 환불 복구 배치가 같은 요청과 멱등키로 이어서 처리한다.
- 성공하면 결제 시도는 `COMPENSATED`가 된다. PG의 명시적 최종 거절만 `COMPENSATION_FAILED`로 전이한다.
- 큐 거절·서킷 오픈·일시 오류와 타임아웃·통신 단절은 `COMPENSATION_REQUESTED`를 유지하고 환불 복구 배치가 같은 멱등키로 다시 확인한다.
- 조치가 필요한 보상 환불은 기존 관리자 환불 목록과 재처리 API를 그대로 사용한다.
- amount=0 내부 승인은 외부 결제가 없으므로 로컬 생성 실패를 `FAILED`로 기록하고 보상 환불을 만들지 않는다.
- 보상 요청 트랜잭션 자체가 실패하면 원래 로컬 실패 예외에 보상 실패를 suppressed 원인으로 보존하고
  ERROR 로그를 남긴다. 이때 보상 이력은 아직 durable하지 않지만 결제 시도는 `PROCESSING` 또는 `APPROVED`에
  남으므로 자동 복구 기준점으로 사용한다.
- `BatchScheduler`는 매분 45초마다 1분 이상 지난 `PROCESSING`·`RETRYABLE`·`APPROVED`를 오래된 순으로 최대 10건 조회한다.
  건별 처리 직전에 행 잠금 아래 상태와 시각을 다시 검증하고, 암호화된 저장 payload의 결제 주체와 저장된
  paymentKey·orderId·amount로 기존 confirm 명령을 복원한다.
- 각 시도는 `confirm_recovery_attempted_at`을 먼저 저장한다. 다음 배치는 이 시각도 1분 이상 지난 건만 선택하고
  마지막 처리 시각 순으로 정렬하므로 PG 장애나 영구 오류가 같은 10개 슬롯을 계속 독점하지 않는다.
- stale `PROCESSING/RETRYABLE`은 새 processing token으로 재선점한 뒤 같은 orderId 멱등키로 PG confirm을 재확인한다.
  stale `APPROVED`는 PG를 다시 호출하지 않고 fulfillment부터 재개한다. fulfillment가 다시 실패하면 기존
  보상 트랜잭션이 `COMPENSATION_REQUESTED`와 환불 `REQUESTED`를 함께 저장하고 환불 복구 경로로 넘긴다.
- Toss가 동일 멱등 응답을 보존하는 15일보다 여유를 둬, 생성 후 14일 이내인 `PROCESSING/RETRYABLE`만 PG에
  자동 재확인한다. 그보다 오래된 시도는 외부 승인 여부를 추측하지 않고 `RECONCILIATION_REQUIRED`로 전이해
  로컬 `FAILED` 오판과 보상 누락을 막는다. 동일한 상한은 사용자 confirm 재호출에도 적용하며, 상태 전이는
  별도 트랜잭션에 커밋한 뒤 `PAYMENT_RECONCILIATION_REQUIRED`를 반환한다. 이미 승인 키가 저장된 `APPROVED` fulfillment 재개에는
  이 상한을 적용하지 않는다. 외부 PG 호출이 없는 amount=0 `PROCESSING`도 기간과 무관하게 내부 처리를 재개한다.
- `RECONCILIATION_REQUIRED` 전이 시 `happygallery.payment.confirm.reconciliation_required` 카운터를 올리고
  Prometheus critical 알림을 발생시킨다. 운영자는 관리자 결제 대사 API에서 저장된 orderId로 Toss 조회 API를
  호출한다. 조회는 활성 DB 트랜잭션 밖에서 수행하고, 결과 저장은 결제 시도 행을 잠그는 짧은 새 트랜잭션으로
  분리한다. `DONE`은 저장된 paymentKey·orderId·금액을 모두 대조한 뒤 `APPROVED`와 fulfillment를 재개한다.
  Toss가 `NOT_FOUND_PAYMENT`로 결제 미존재를 명시한 경우에만 `FAILED`로 종결하고 payload를 제거한다.
  다른 404, 조회 실패, 자동 판정할 수 없는 상태는 `RECONCILIATION_REQUIRED`를 유지한다. 자동 복구는 이 상태를 다시 처리하지 않는다.
- Toss `PAYMENT_STATUS_CHANGED` 웹훅은 `transmission-id` 유일키로 수신 기록하고 알려진 결제 시도에만 연결한다.
  웹훅 본문을 상태 확정 근거로 쓰지 않으며, 매분 배치가 기존 Toss 조회 대사를 실행한다. 중복 웹훅은 같은 영수증 행에서
  제거하고 처리 중 중단된 영수증은 1분 뒤 다시 선점한다.
- confirm을 시작하지 않은 `PENDING`은 30분 유효시간을 둔다. confirm 진입과 만료 배치 모두 행 잠금 아래
  같은 UTC `created_at` 경계를 확인하고, 만료 시 `CANCELED` 전이와 암호화 payload 제거를 먼저 커밋한다. confirm은 payload
  복호화와 PG 호출을 시도하지 않고 `PAYMENT_ATTEMPT_EXPIRED`를 반환하며, 배치는 confirm 요청이 없는 레코드를 일괄 정리한다.
- `CONFIRMED`, `FAILED`, `COMPENSATED`, `CANCELED`은 생성 30일 뒤 `payload_enc`,
  `fulfilled_access_token_enc`, `owner_phone_hmac`과 `status_access_token_hash`를 제거한다. 상태·금액·PG 식별자·도메인 ID는 감사에 남기고,
  `RECONCILIATION_REQUIRED`와 보상 진행 상태는 복구 가능성을 위해 정리하지 않는다. 정리 후 재조회는
  `PAYMENT_RESULT_RETENTION_EXPIRED`로 명확히 종료한다.
- 환불 성공 시각은 `refunds.succeeded_at`에 별도로 저장한다. 고객 순매출은 환불 요청·실패 시점이 아니라
  이 성공 시각에만 차감하며, 도메인 생성 전 승인 취소인 보상환불은 고객 매출·환불 통계에서 제외한다.
- 후보 조회는 잠그지 않지만 실제 confirm의 비관적 잠금과 processing token fencing을 그대로 사용한다.
  따라서 사용자 요청이나 여러 서버의 복구 배치가 겹쳐도 PG 결과와 도메인 생성은 한 실행권만 반영한다.
  `PENDING`과 최종·보상 상태는 자동 confirm 대상이 아니다.

### 7. 고객 결제 상태 조회는 prepare 소유권으로 제한한다

PG 승인이 끝난 뒤 fulfillment가 실패하면 confirm HTTP 응답은 원래 도메인 예외로 끝날 수 있지만,
외부 결제는 이미 성립했고 보상 환불은 비동기로 진행된다. 이 상태를 일반 결제 실패로 표시해 재결제를
유도하지 않도록 고객용 결제 상태 조회를 제공한다.

- 회원 prepare는 `payment_attempt.owner_user_id`를 저장하고 조회 시 현재 고객 세션과 비교한다.
- 비회원 prepare는 30일 만료 HMAC 서명 토큰을 발급한다. 원문은 응답으로만 전달하고
  `payment_attempt.status_access_token_hash`에는 서명 토큰 전체의 SHA-256 해시만 저장한다. 별도로 정규화 휴대폰의
  active HMAC을 `owner_phone_hmac`에 저장해 브라우저 저장소 전체 유실에도 SMS 소유 확인으로 결제 목록을 찾는다.
- `orderId`는 조회 키일 뿐 인증 자격이 아니다. 결제 미존재와 소유권 불일치는 모두 `NOT_FOUND`로 처리한다.
- SMS 소유 확인 복구는 active·previous 휴대폰 HMAC 후보로 최근 30일 최종 결제와 미종결 결제를 찾고 ID 순으로
  잠근 뒤 공통 새 상태 조회 토큰으로 모두 교체한다. 응답이 `orderId` 목록을 함께 주므로 기존 orderId가 없어도
  복구할 수 있고, 이전 상태 조회 토큰은 즉시 무효가 된다.
- 고객 응답은 진행 단계와 금액, 완료된 도메인 연결 정보만 제공한다. 내부 실패 사유, 환불 ID와 PG 키는
  관리자 복구 정보이므로 노출하지 않는다.
- confirm 응답이 유실된 비회원 결제가 `CONFIRMED`이면 기존 암호문에서 주문·예약 접근 토큰을 복원해 반환한다.

### 8. 고객 영수증과 PG 정산 대사는 결제 원장에 연결한다

- Toss 승인 응답의 `receipt.url`은 `payment_attempt.confirmed_receipt_url`에 저장하고 결제 완료·상태 조회 응답에 포함한다. 주문·예약·8회권에 같은 값을 복제하지 않는다.
- Toss 정산 API는 최대 60초가 걸릴 수 있으므로 confirm·cancel의 3초 풀과 분리한 전용 커넥션 풀을 사용한다. 인증 키와 base URL만 공유한다.
- 매시간 최근 7일 정산을 다시 읽고 거래키로 upsert한다. 승인 거래는 `paymentKey`·`orderId`·금액, 취소 거래는 취소 `transactionKey`·금액을 로컬 원장과 비교한다.
- 불일치는 `payment_settlements.reconciliation_status`와 사유로 유지하고 관리자 화면에 표시한다. 외부 조회 중에는 DB 트랜잭션을 열지 않고 각 거래 반영만 짧은 새 트랜잭션에서 수행한다.

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
| 장점 | 고객이 재결제하지 않고 승인·보상환불 진행 결과를 안전하게 확인할 수 있다 |
| 장점 | 비회원이 브라우저 저장소 전체를 잃어도 SMS 소유 확인으로 결제 orderId와 상태 조회 자격을 함께 복구한다 |
| 장점 | 보상 요청 저장 트랜잭션까지 실패해도 남은 결제 중간 상태를 배치가 자동 재개한다 |
| 단점 | confirm 상태와 보상 상태가 늘어나 운영 조회가 복잡해진다 |
| 단점 | 비회원 confirm 재응답을 위해 결제 시도 보존 기간 동안 접근 토큰 암호문을 추가 관리한다 |

## 회원 탈퇴와 prepare 경합

- 회원 결제 prepare는 `users` 행을 잠그고 활성 회원인지 확인한 뒤 `payment_attempt.owner_user_id`를 저장한다.
- 회원 탈퇴도 같은 회원 행을 잠그고 미종결 결제 시도를 포함한 차단 활동을 다시 확인한다. 따라서 새 prepare와 탈퇴 중 하나만 먼저 커밋한다.
- prepare가 먼저 커밋되면 미종결 결제 시도가 탈퇴를 차단한다. 탈퇴가 먼저 커밋되면 이후 prepare는 탈퇴 회원으로 거절된다.
- 이미 prepare된 시도와 탈퇴가 예외적으로 엇갈려 PG 승인이 도착하더라도 fulfillment는 현재 회원 상태를 다시 확인한다. 탈퇴 회원의 주문·예약·8회권을 만들지 않고 기존 `payment_attempt_id` 보상 환불 경계로 종결한다.

## 구현 반영

- `PaymentConfirmClaimTransactionService`
- `PaymentConfirmFulfillmentTransactionService`
- `PaymentConfirmRecoveryTransactionService`
- `PaymentConfirmAttemptResolver`
- `DefaultPaymentConfirmService`
- `DefaultPaymentConfirmRecoveryService`, `PaymentConfirmRecoveryUseCase`, `BatchScheduler`
- `DefaultPaymentAttemptExpiryBatchService`, `PaymentAttemptExpiryProcessor`
- `DefaultPaymentReconciliationAdminService`, `PaymentReconciliationTransactionService`
- `PaymentPreparer`, `PaymentFulfiller`, `OrderPreparer`, `OrderFulfiller`, `BookingFulfiller`, `PassFulfiller`
- `PaymentPayload`, `PreparedPaymentPayload`
- `ProductReaderPort`, `ProductRepository`, `CartUseCase`
- `PaymentAttempt`, `PaymentAttemptStatus`
- `DefaultPaymentStatusQueryService`, `PaymentStatusQueryUseCase`, `PaymentQueryController`
- `RefundExecutionService`, `RefundTransactionService`, `Refund`
- `TossPaymentsProvider`
- `TossPaymentSettlementProvider`, `DefaultPaymentSettlementService`, `PaymentSettlement`
- `V155__store_payment_receipt_url.sql`, `V156__reconcile_payment_settlements.sql`
- `V41__harden_payment_confirm_boundary.sql`
- `V46__ProtectPlaintextPersonalData`
- `V49__persist_payment_confirm_result.sql`
- `V50__fence_payment_confirm_processing.sql`
- `V51__track_payment_confirm_recovery.sql`
- `V58__track_refund_success_time.sql`
- `V60__normalize_revenue_timestamps_to_seoul.sql`
- `V76__secure_payment_attempt_status_lookup.sql`
