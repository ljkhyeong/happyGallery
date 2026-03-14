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
    └─ 24h 초과 배치 → AUTO_REFUND_TIMEOUT
```

이미 환불된 상태(REJECTED, AUTO_REFUND_TIMEOUT, PICKUP_EXPIRED)에서
승인/거절 재시도 → `AlreadyRefundedException` (409).
이 가드는 기존 `OrderStatus.requireApprovable()`을 재사용한다.

### 2. 환불·재고 복구 순서

거절/자동환불 시 반드시 **재고 복구 → 환불 기록 생성 → PG 환불 호출** 순으로 처리한다.
PG 호출 실패 시 환불 레코드가 FAILED로 남아 운영자 재처리 대상이 된다.

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
별도 엔티티 없이 `Refund(Long orderId, long amount)` 생성자를 추가하는 것으로 충분하다.

### 7. 승인 이력에 관리자 식별자 기록

관리자 승인/거절 및 제작 완료 이력은 `order_approvals.decided_by_admin_id`를 함께 저장한다.
현재는 `X-Admin-Id` 헤더를 선택적으로 받아 null 허용으로 운영하고,
배치 자동환불(`AUTO_REFUND`)도 null 이력을 허용한다.

---

## 결과 (위험 포인트)

| 항목 | 내용 |
|------|------|
| 멱등성 | 상태 가드 + 낙관적 락 + 제한 재시도로 이중 처리 가능성을 낮춘다. 재시도 소진 시 해당 건은 실패 집계에 남기고 다음 건으로 진행 |
| PG 환불 실패 | FAILED 레코드 적체 알림 없음 (ADR-0008 동일 이슈). §11에서 알림 연동 |
| 배치 단위 트랜잭션 | `autoRefundExpired()`는 목록 조회 후 건별 `REQUIRES_NEW` 트랜잭션으로 처리한다. 추후 건수 증가 시 페이지네이션 검토 필요 |
| 승인 기한 경과 후 관리자 승인 | 배치 미실행 상태에서 기한 경과 주문도 관리자가 승인 가능 (의도된 여유). 배치 실행 후에는 409 차단 |
| 관리자 식별자 null 허용 | 인증 체계 완성 전까지 이력에 null adminId가 저장될 수 있음. 보안 모델 확정 시 필수화 검토 필요 |
