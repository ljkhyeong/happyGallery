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
    ├─ 관리자 거절 → REJECTED_REFUNDED
    └─ 24h 초과 배치 → AUTO_REFUNDED_TIMEOUT
```

이미 환불된 상태(REJECTED_REFUNDED, AUTO_REFUNDED_TIMEOUT)에서
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

### 4. 배치 스케줄 미연결

`@Scheduled` 연결은 §10에서 수행한다. 현재는 서비스만 구현됨.

### 5. 환불 엔티티 재사용

주문 환불은 기존 `Refund` 엔티티(refunds 테이블)에 `orderId` 필드를 통해 기록한다.
별도 엔티티 없이 `Refund(Long orderId, long amount)` 생성자를 추가하는 것으로 충분하다.

---

## 결과 (위험 포인트)

| 항목 | 내용 |
|------|------|
| 멱등성 | `requireApprovable()` 가드로 이중 처리 방지. 단, 배치 트랜잭션 실패 후 재실행 시 재고 이중 복구 가능 — §10 배치 연결 시 재검토 필요 |
| PG 환불 실패 | FAILED 레코드 적체 알림 없음 (ADR-0008 동일 이슈). §11에서 알림 연동 |
| 배치 단위 트랜잭션 | 현재 `autoRefundExpired()`가 단일 트랜잭션. 건수 많아지면 페이지네이션 + 건별 트랜잭션 분리 필요 |
| 승인 기한 경과 후 관리자 승인 | 배치 미실행 상태에서 기한 경과 주문도 관리자가 승인 가능 (의도된 여유). 배치 실행 후에는 409 차단 |
