# ADR-0014: 예약 제작 주문 구현 결정 (§8.3)

**날짜**: 2026-03-03
**상태**: Accepted

---

## 컨텍스트

spec.md §3.2: MADE_TO_ORDER 상품 주문이 승인되면 즉시 제작 시작(IN_PRODUCTION).
제작 시작 이후 일반 취소/거절 환불은 불가하다. 예상 출고일을 관리자가 설정·노출한다.
관리자가 지연을 제안하면 고객 응답 대기 상태로 전환하고, 고객이 수락하면 `DELAY_ACCEPTED`,
거절하면 별도 취소 상태로 전환한다.
제작이 완료되면 주문은 픽업/배송 공통 이행 흐름(APPROVED_FULFILLMENT_PENDING)으로 다시 합류해야 한다.
회원 바로 주문·장바구니와 비회원 주문 모두 결제 전에 픽업 또는 배송을 선택하므로, 제작 완료 뒤 관리자가
수령 방식을 임의로 바꾸지 않고 결제 시점 선택을 유지해야 한다.

---

## 결정 사항

### 1. 상태 흐름

```

`MADE_TO_ORDER`가 포함된 주문은 결제 전에 별도 동의를 받아야 한다. 공개 요청은 화면이 조회한 문구 버전과 동의 여부를 보내고,
서버는 현재 버전과 일치할 때만 고지 문구 버전·전문·동의 시각을 결제 준비 payload에 확정한 뒤 confirm에서 `orders`에 저장한다.
동의 스냅샷이 없는 주문은 `approveAsProduction()`이 제작 시작을 거절한다. 이 기록은 주문제작 청약철회 제한
고지를 입증하기 위한 것이며 하자·오배송 등 법령상 권리를 일률적으로 배제하지 않는다.
MADE_TO_ORDER 주문 결제
    ↓
PAID_APPROVAL_PENDING
    ↓ 관리자 승인 (approve)
IN_PRODUCTION          ← 환불 불가 시작점
    ├─ 예상 출고일 설정 (setExpectedShipDate) → 상태 변화 없음
    ├─ 관리자 지연 제안 (proposeDelay)
    │      ↓
    │  DELAY_CONSENT_PENDING
    │      ├─ 고객 수락 → DELAY_ACCEPTED
    │      │      ├─ 제작 재개 (resumeProduction) → IN_PRODUCTION
    │      │      └─ 제작 완료 (completeProduction) → APPROVED_FULFILLMENT_PENDING
    │      └─ 고객 거절 → DELAY_REJECTED_CANCELED
    └─ 제작 완료 (completeProduction)
           ↓
    APPROVED_FULFILLMENT_PENDING
    ├─ 픽업 준비 → PICKUP_READY → PICKED_UP / PICKUP_FORFEITED(미수령, 환불 없음)
    └─ 배송 준비 (prepareShipping)
           ↓
    SHIPPING_PREPARING
           ↓ 배송 출발 (markShipped)
    SHIPPED
           ↓ 배송 완료 (markDelivered)
    DELIVERED
```

READY_STOCK 상품은 기존 흐름 유지: approve → APPROVED_FULFILLMENT_PENDING.

### 2. 제품 유형 감지 위치

`OrderApprovalService.approve()` 내부에서 OrderItem → Product 조회로 MADE_TO_ORDER 여부를 판단한다.
`Order` 엔티티는 product type을 직접 알지 않으며, 서비스 레이어에서 판단 후 `approveAsProduction()` 또는 `approve()`를 선택 호출한다.

### 3. Fulfillment와 배송지 스냅샷 생성

결제 confirm에서 주문·주문 항목·재고 차감과 같은 트랜잭션으로 `fulfillments` 레코드를 생성한다.

- `type`은 prepare 전에 고객이 선택한 `SHIPPING` 또는 `PICKUP`으로 고정한다.
- `SHIPPING`은 받는 사람·표준화 전화번호·우편번호·기본/상세 주소를 JSON으로 직렬화하고 AES-GCM 암호문만 `shipping_address_enc`에 저장한다.
- `PICKUP`은 현재 단일 매장 정책이므로 매장명 자유 문자열이나 배송지를 저장하지 않는다.
- `expected_ship_date`와 `pickup_deadline_at`은 최초 null이며 관리자가 해당 타입의 후속 단계에서 설정한다.
- Fulfillment에 별도 `status` 컬럼은 없다. 주문 상태는 `Order.status`가 단일 소스다.
- `order_id`는 unique로 유지해 주문당 fulfillment 1건 불변식을 보장한다.

관리자 주문 목록은 `type`만 일괄 조회하고, 배송지 원문은 관리자 단건 이행 상세 API에서만 복호화한다.
`OrderShippingService`와 `OrderPickupService`는 저장된 타입을 먼저 확인해 고객 선택과 다른 상태 전이를 거절한다.

### 4. 환불 불가 가드

`OrderStatus.requireCancellable()` — IN_PRODUCTION, DELAY_CONSENT_PENDING 또는 DELAY_ACCEPTED 상태에서 호출 시
`ProductionRefundNotAllowedException`(422 `PRODUCTION_REFUND_NOT_ALLOWED`)을 던진다.

`Order.reject()`에 이 가드를 추가하여 제작 중 거절을 차단한다.

고객이 관리자 지연 제안을 거절한 경우는 일반 취소가 아니라 지연 거절 취소로 다룬다.
`OrderStatus.requireDelayRejectionCancelable()`은 `DELAY_CONSENT_PENDING`에서만 통과하고,
서비스는 주문을 `DELAY_REJECTED_CANCELED`로 전이한 뒤 환불·재고 복구를 수행한다.
이미 `DELAY_ACCEPTED`로 전이된 주문은 고객이 지연을 수락한 상태이므로 이 경로를 허용하지 않는다.

제작 완료 후 픽업 흐름에 합류해도 제작 시작 이력은 사라지지 않는다. 픽업 마감까지 미수령하면
`PICKUP_FORFEITED` 상태와 이력만 남기고 `Refund` 생성과 재고 복구는 수행하지 않는다.

### 5. 서비스 분리

- `OrderApprovalService`: approve (MADE_TO_ORDER 감지 포함) / reject
- `OrderProductionService`: setExpectedShipDate / proposeDelay / resumeProduction / completeProduction
- `OrderCustomerActionService`: 회원·비회원 소유권 검증 후 지연 수락 또는 거절 처리
- `OrderShippingService`: prepareShipping / markShipped / markDelivered

### 6. API

| Method  | Path                                    | 설명                        |
|---------|-----------------------------------------|-----------------------------|
| `PATCH` | `/api/v1/admin/orders/{id}/expected-ship-date` | 예상 출고일 설정/갱신        |
| `POST`  | `/api/v1/admin/orders/{id}/delay`              | 제작 지연 제안 (`IN_PRODUCTION` → `DELAY_CONSENT_PENDING`) |
| `POST`  | `/api/v1/me/orders/{id}/delay-response` | 회원이 지연 제안 수락/거절 |
| `POST`  | `/api/v1/orders/{id}/delay-response` | 비회원이 접근 토큰으로 지연 제안 수락/거절 |
| `POST`  | `/api/v1/admin/orders/{id}/cancel-for-delay-rejection` | 관리자 보조 지연 거절 취소 |
| `POST`  | `/api/v1/admin/orders/{id}/resume-production`  | 지연 수락 → 제작 재개 (DELAY_ACCEPTED → IN_PRODUCTION) |
| `POST`  | `/api/v1/admin/orders/{id}/complete-production`| 제작 완료 → 이행 대기 상태 복귀 |
| `POST`  | `/api/v1/admin/orders/{id}/prepare-shipping`   | 배송 준비 시작 (APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING) |
| `POST`  | `/api/v1/admin/orders/{id}/mark-shipped`       | 택배사·운송장 번호를 저장하고 배송 출발 |
| `POST`  | `/api/v1/admin/orders/{id}/mark-delivered`     | 배송 완료 (SHIPPED → DELIVERED) |
| `GET`   | `/api/v1/admin/orders/{id}/fulfillment`        | 관리자 이행 방식·배송지 상세 조회 |
| `GET`   | `/api/v1/admin/orders/{id}/history`            | 주문 처리 이력 조회 |

관리자 주문 처리 API는 Bearer 세션에서 검증된 admin id를 `order_approvals`에 기록한다.
`setExpectedShipDate`는 `IN_PRODUCTION`, `DELAY_CONSENT_PENDING`, `DELAY_ACCEPTED`, `SHIPPING_PREPARING` 상태의 SHIPPING fulfillment에서만 허용한다.
`Fulfillment.setExpectedShipDate()`가 SHIPPING 타입을 직접 검증해 변경 메서드 밖의 중복 사전 검증을 두지 않는다.
`mark-shipped`는 `carrier`와 `trackingNumber`를 필수로 받고 둘을 한 쌍으로 저장한다. 픽업 fulfillment에는
운송 정보를 저장할 수 없다.

---

## 결과 (위험 포인트)

| 항목 | 내용 |
|------|------|
| 혼합 주문 | MADE_TO_ORDER + READY_STOCK 상품이 같은 주문에 있으면 전체를 제작 주문으로 보아 IN_PRODUCTION으로 전이하고, 픽업 미수령 시에도 전체 주문을 환불하지 않는다. |
| Fulfillment 상태 관리 | Fulfillment에 별도 `status` 컬럼은 없고 `Order.status`가 단일 소스다. 수령 방식은 결제 시점에 고정하며 제작 완료 뒤에도 변환하지 않는다. |
| 배송지 노출 | 배송지 스냅샷은 암호문만 저장하고 고객 상세·관리자 목록에는 포함하지 않는다. 관리자 단건 이행 상세만 복호화한다. |
| 배송·픽업 이력 관리 | 배송과 픽업 전이도 `order_approvals` append-only 이력으로 남기며, 운영 화면은 이를 시간순 조회한다. |
| 관리자 식별자 | Bearer 세션 경로는 admin id를 이력에 기록하고, API Key 폴백 경로는 null 이력이 존재할 수 있다. |
