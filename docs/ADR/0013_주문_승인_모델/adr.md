# ADR-0013: 주문 승인 모델 (§8.2)

**날짜**: 2026-03-02
**상태**: Accepted

---

## 컨텍스트

오프라인 공방의 온라인 판매 정책상 주문이 즉시 확정되지 않는다.
결제 완료 후 관리자가 수기로 재고·일정을 확인한 뒤 승인/거절을 결정한다.
또한 관리자가 24시간 내 응답하지 않으면 자동으로 환불해야 한다.

---

## 결정 사항

### 1. 상태 흐름

```
결제 완료
    ↓
PAID_APPROVAL_PENDING  (approval_deadline_at = paidAt + 24h)
    ├─ 관리자 승인 → APPROVED_FULFILLMENT_PENDING
    ├─ 관리자 거절 → REJECTED
    ├─ 고객 취소 → CUSTOMER_CANCELED
    └─ 24h 초과 배치 → AUTO_REFUND_TIMEOUT
```

고객 취소는 제작·이행 시작 전인 `PAID_APPROVAL_PENDING`에서만 허용한다. 회원은 세션의 본인 주문,
비회원은 주문 접근 토큰으로 소유권을 확인한다. 관리자 거절 의미인 `REJECTED`를 재사용하지 않는다.

이미 환불된 상태(REJECTED, CUSTOMER_CANCELED, AUTO_REFUND_TIMEOUT, PICKUP_EXPIRED, DELAY_REJECTED_CANCELED)에서
승인/거절 재시도 → `AlreadyRefundedException` (409).
이 가드는 `OrderStatus.requireApprovalPending()`에서 적용한다.

주문제작 미수령은 환불된 기성품과 구분해 `PICKUP_FORFEITED`로 종결한다.
승인/거절 재시도는 `ALREADY_REFUNDED`가 아니라 승인 대기 상태가 아닌 요청으로 거절한다.

### 2. 환불·재고 복구 순서

거절/고객 취소/자동환불 트랜잭션에서는 **상태·이력 기록, 재고 복구, 환불 요청 생성**을
한 트랜잭션으로 처리하고,
커밋 이후 PG 환불을 호출한다. 명시적 거절은 `FAILED`, 일시 실패와 결과 불명은 자동 복구 가능한 상태로 남는다.

픽업 만료는 기성품 주문에만 같은 보상 흐름을 적용한다. 주문제작 상품이 하나라도 포함된 주문은
미수령 상태와 이력만 `PICKUP_FORFEITED`로 종결하고 환불 이력과 재고 복구를 만들지 않는다.

### 3. 서비스 분리

- `OrderApprovalService`: approve / reject (관리자 액션)
- `OrderAutoRefundBatchService`: 24h 초과 자동환불 배치

배치 서비스는 `OrderApprovalService`의 `restoreInventory()` / `processRefund()`를 재사용하여
환불 로직을 단일 경로로 유지한다.

### 4. 동시성 전략: 낙관적 락 + 제한 재시도

`orders.version`, `fulfillments.version` 컬럼을 두고
주문 승인, 자동환불, 픽업 만료 같은 운영 액션 충돌 구간은 `@Version` 기반 낙관적 락을 사용한다.

- 수동 승인/거절과 배치 자동환불이 같은 주문을 동시에 수정하면 `ObjectOptimisticLockingFailureException`으로 감지한다.
- 자동환불/픽업 만료 배치는 건별 `REQUIRES_NEW` 트랜잭션으로 처리한다.
- 충돌은 `@Retryable(maxAttempts=3)`로 최대 3회 재시도하고, 모두 실패하면 해당 건만 스킵한다.

이 결정은 재고처럼 경쟁이 빈번한 row에는 비관적 락을 유지하면서,
운영성 상태 전이만 가볍게 충돌 감지하려는 목적이다.

### 5. 배치 스케줄 연결 + 공통 로깅

배치는 `@Scheduled`로 연결하고, `@BatchJob` + AOP로 시작/완료/실패 로그를 공통 처리한다.
반환값은 `BatchResult(successCount, failureCount, failureReasons)`를 사용해
성공 건수와 실패 사유 집계를 함께 남긴다.
외부(Admin) 응답에서는 내부 예외명을 직접 노출하지 않고,
`CONFLICT`, `NOT_FOUND`, `ALREADY_PROCESSED`, `BUSINESS_ERROR`, `INTERNAL_ERROR`로 정규화한다.

### 6. 환불 엔티티 재사용

주문 환불은 기존 `Refund` 엔티티(refunds 테이블)에 `orderId` 필드를 통해 기록한다.
별도 엔티티 없이 `Refund.forOrder(orderId, amount, paymentKey)` 팩토리로 기록한다.
픽업 만료 환불 이력은 기성품 주문에만 생성한다. `PICKUP_EXPIRED`는 환불 경로,
`PICKUP_FORFEITED`는 주문제작 미환불 경로를 나타낸다.

### 7. 주문 처리 이력에 관리자 식별자 기록

관리자 승인/거절, 제작, 픽업, 배송 이력은 `order_approvals.decided_by_admin_id`를 함께 저장한다.
`AdminAuthenticationFilter`가 Bearer 세션을 검증해 `AdminPrincipal`과 `SecurityContext`를 구성하고,
주문 컨트롤러는 `@AuthenticationPrincipal AdminPrincipal`의 `auditActorId()`를 이력에 전달한다.
Bearer 세션은 검증된 관리자 ID를 반환하고, 사람 계정을 나타내지 않는 로컬 API key와
배치 자동환불(`AUTO_REFUND`), 픽업 만료(`PICKUP_EXPIRED`, `PICKUP_FORFEITED`)는 null 이력을 허용한다.

### 8. 픽업 마감 알림의 연관 데이터는 일괄 조회한다

픽업 마감 임박 fulfillment와 주문 수신자를 projection JOIN으로 한 번에 조회한다.
같은 수신자가 여러 주문을 가질 수 있으므로 수신자 단위 최근 발송 이력으로 후보를 제거하지 않는다.
각 주문은 수신자와 무관한 `PICKUP_DEADLINE_REMINDER + ORDER + orderId` outbox 멱등키로 한 번만 저장한다.
따라서 비회원 주문이 회원에게 귀속돼도 동일 주문 알림을 새 요청으로 만들지 않으며,
알림 outbox 저장과 dispatch는 건별 실패 격리·재시도 경계를 유지한다.

### 9. 고객이 선택한 이행 방식을 승인 이후에도 유지한다

- 주문 confirm 시 `SHIPPING` 또는 `PICKUP` fulfillment를 주문과 같은 트랜잭션에 저장한다.
- 관리자는 `SHIPPING` 주문에만 배송 준비를, `PICKUP` 주문에만 픽업 준비를 시작할 수 있다. 반대 흐름은 주문 상태를 바꾸기 전에 거절한다.
- 픽업 마감은 주입된 `Clock` 기준 현재보다 이후인 값만 받는다. 만료 배치는 이후 실제 시간이 경과한 fulfillment만 처리한다.

### 10. 결제 시점 표시·금액 스냅샷을 주문에 유지한다

- `order_items.product_name`과 `unit_price`는 prepare에서 확정한 상품 표시와 단가를 저장한다. 상품명이 바뀌어도 과거 주문에는 결제 당시 이름을 보여준다.
- 배송 주문은 서버 설정 `app.order.shipping-fee`를 결제 금액에 한 번 더하고 `orders.shipping_fee`에 저장한다. 픽업 주문의 배송비는 항상 0원이다.
- 주문 거절·고객 취소·자동 환불·지연 거절의 전액 환불은 배송비가 포함된 `order.totalAmount`를 사용한다.
- 배송 출발 시 `carrier`, `tracking_number`를 함께 받아 fulfillment에 저장하고 고객·관리자 상세에 같은 운송 정보를 반환한다.
- 현재 배송비 정책은 주문 금액과 무관한 고정액이며 `GET /api/v1/orders/policy`로 공개한다. 무료 배송 임계값은 두지 않는다.

---

## 결과 (위험 포인트)

| 항목 | 내용 |
|------|------|
| 멱등성 | 상태 가드 + 낙관적 락 + 제한 재시도로 이중 처리 가능성을 낮춘다. 재시도 소진 시 해당 건은 실패 집계에 남기고 다음 건으로 진행 |
| PG 환불 미완료 | 자동 복구와 관리자 조치 필요 목록으로 추적. PG 실행기 대기열 포화·거절은 별도 알림 |
| 배치 단위 트랜잭션 | `autoRefundExpired()`는 목록 조회 후 건별 `REQUIRES_NEW` 트랜잭션으로 처리한다. 추후 건수 증가 시 페이지네이션 검토 필요 |
| 픽업 알림 조회량 | 후보 주문과 최근 성공 이력은 일괄 조회하고, outbox 처리만 건별 격리를 유지 |
| 승인 기한 경과 후 관리자 승인 | 배치 미실행 상태에서 기한 경과 주문도 관리자가 승인 가능 (의도된 여유). 배치 실행 후에는 409 차단 |
| 관리자 식별자 null 허용 | Bearer 세션 경로는 adminId가 기록되지만, API Key 폴백과 자동환불·픽업 만료 배치 이력은 null일 수 있음 |
