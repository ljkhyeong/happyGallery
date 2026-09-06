---
name: happygallery-order-flows
description: happyGallery의 주문 승인·제작·배송·픽업·장바구니 구매·반품·교환·주문 환불 정책을 변경할 때 사용한다. PG 호출·재시도가 주목적이면 결제 스킬을 사용한다.
---

# happyGallery 주문·클레임

## 규칙

- `application/src/main/java/com/personal/happygallery/application/order/`와 `cart/`, PRD-0001, ADR-0012~0014·0039를 확인한다.
- 가격 조회·재고 변경·상태 전이·환불 요청·알림을 구분한다. 상품·재고·장바구니는 일괄 조회하며 상태 전이는 도메인 메서드를 사용한다.
- 회원 활동을 만들 때 `MemberAccountGuard`로 회원 행을 주문·클레임 행보다 먼저 잠근다. 소유자는 factory와 DB 제약에서 정확히 하나로 제한한다.
- 승인 기한 초과 자동 환불·재고 복원, 제작 시작 후 취소 제한을 유지한다. 재고 상품의 픽업 만료 자동 환불에 주문 제작 픽업을 포함하지 않는다.
- 결제 prepare가 저장한 단가를 `OrderItemRequest`로 전달한다. fulfillment에서 변경된 상품 가격을 다시 읽지 않는다.
- 장바구니는 ORDER prepare/confirm을 사용한다. 서버가 장바구니 항목 ID·상품 ID·수량·단가를 저장하고, 완료 시 회원·항목 ID를 잠가 준비한 수량만 제거한다. 이후 추가한 수량과 새로 만든 같은 상품 행은 보존한다.
- 완료 후 클레임은 주문 항목 ID·수량으로 받는다. 주문을 잠그고 거절되지 않은 누적 수량을 확인한다. 직접 주문 환불과 클레임별 환불을 구분한다.
- 클레임 항목과 환불이 같은 주문에 속하도록 복합 FK를 유지한다. 여러 항목 환불은 최대 나머지 방식의 비례 배분으로 승인 합계를 보존하고 항목별 금액을 저장한다. 배송비 환불은 상품 매출에서 분리한다.
- 반품 재입고 여부는 관리자가 명시한다. 판매 가능 반품만 복원하고, 교환 승인 시 교환 상품 재고를 차감한다.
- 환불 클레임은 PG 성공을 저장하는 트랜잭션에서 완료한다. 교환 완료에는 택배사·송장 번호·처리 관리자를 기록한다.
- 외부 결제 실패가 이미 필요한 주문 거절·취소를 되돌리지 않게 한다. 실행·보상은 `happygallery-payment-flows`를 따른다.

## 검증

- 상태·재고 규칙은 해당 policy test를 실행한다.
- 흐름은 변경에 맞게 `OrderApprovalUseCaseIT`, `OrderProductionUseCaseIT`, `OrderClaimUseCaseIT`, `PickupExpireBatchUseCaseIT`, `ConcurrentOrderUseCaseIT`를 선택한다.
