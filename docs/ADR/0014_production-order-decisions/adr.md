# ADR-0014: 예약 제작 주문 구현 결정 (§8.3)

**날짜**: 2026-03-03
**상태**: Accepted

---

## 컨텍스트

spec.md §3.2: MADE_TO_ORDER 상품 주문이 승인되면 즉시 제작 시작(IN_PRODUCTION).
제작 시작 이후 환불 불가. 예상 출고일을 관리자가 설정·노출.
고객 동의 시 DELAY_REQUESTED 상태로 전환 가능.
제작이 완료되면 주문은 픽업/배송 공통 이행 흐름(APPROVED_FULFILLMENT_PENDING)으로 다시 합류해야 한다.

---

## 결정 사항

### 1. 상태 흐름

```
MADE_TO_ORDER 주문 결제
    ↓
PAID_APPROVAL_PENDING
    ↓ 관리자 승인 (approve)
IN_PRODUCTION          ← 환불 불가 시작점
    ├─ 예상 출고일 설정 (setExpectedShipDate) → 상태 변화 없음
    ├─ 고객 동의 지연 (requestDelay)
           ↓
    DELAY_REQUESTED
    └─ 제작 완료 (completeProduction)
           ↓
    APPROVED_FULFILLMENT_PENDING
           ↓
    픽업 준비/배송 준비 등 기존 이행 흐름으로 합류
```

READY_STOCK 상품은 기존 흐름 유지: approve → APPROVED_FULFILLMENT_PENDING.

### 2. 제품 유형 감지 위치

`OrderApprovalService.approve()` 내부에서 OrderItem → Product 조회로 MADE_TO_ORDER 여부를 판단한다.
`Order` 엔티티는 product type을 직접 알지 않으며, 서비스 레이어에서 판단 후 `approveAsProduction()` 또는 `approve()`를 선택 호출한다.

### 3. Fulfillment 레코드 생성

MADE_TO_ORDER 승인 시 `fulfillments` 테이블에 레코드를 생성한다.
- `type = SHIPPING` (예약 제작은 배송)
- `status = IN_PRODUCTION` (주문 상태와 동기화)
- `expected_ship_date = null` (관리자가 별도 설정)

READY_STOCK 승인 시에는 Fulfillment를 생성하지 않는다 (§8.4 픽업에서 처리 예정).

### 4. 환불 불가 가드

`OrderStatus.requireCancellable()` — IN_PRODUCTION 또는 DELAY_REQUESTED 상태에서 호출 시
`ProductionRefundNotAllowedException`(422 `PRODUCTION_REFUND_NOT_ALLOWED`)을 던진다.

`Order.reject()`에 이 가드를 추가하여 제작 중 거절을 차단한다.

### 5. 서비스 분리

- `OrderApprovalService`: approve (MADE_TO_ORDER 감지 포함) / reject
- `OrderProductionService`: setExpectedShipDate / requestDelay

### 6. API

| Method  | Path                                    | 설명                        |
|---------|-----------------------------------------|-----------------------------|
| `PATCH` | `/admin/orders/{id}/expected-ship-date` | 예상 출고일 설정/갱신        |
| `POST`  | `/admin/orders/{id}/delay`              | 배송 지연 상태 전환 (고객 동의) |
| `POST`  | `/admin/orders/{id}/complete-production`| 제작 완료 → 이행 대기 상태 복귀 |

`complete-production` 호출 시 `X-Admin-Id` 헤더(선택)를 받아
`order_approvals`에 `PRODUCTION_COMPLETE` 이력을 남긴다.

---

## 결과 (위험 포인트)

| 항목 | 내용 |
|------|------|
| 혼합 주문 | MADE_TO_ORDER + READY_STOCK 상품이 같은 주문에 있으면 전체가 IN_PRODUCTION으로 전이됨. MVP에서는 이런 케이스가 없다고 가정. |
| Fulfillment 상태 동기화 | `requestDelay()`와 `completeProduction()` 모두 Fulfillment.status를 명시적으로 syncStatus()로 갱신한다. |
| 관리자 식별자 | `X-Admin-Id`가 선택 헤더라 null 이력이 존재할 수 있다. 인증/인가 체계 도입 시 필수화 여부를 재검토한다. |
