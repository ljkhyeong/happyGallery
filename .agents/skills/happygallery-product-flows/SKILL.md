---
name: happygallery-product-flows
description: happyGallery의 상품 등록·조회·수정, 재고 차감·복원, 상품 Q&A를 변경할 때 사용한다. 주문 승인·배송·클레임이 주목적이면 주문 스킬을 사용한다.
---

# happyGallery 상품·재고

## 규칙

- `application/src/main/java/com/personal/happygallery/application/product/`와 `qna/`, PRD-0001, ADR-0012·0013·0014를 확인한다.
- 상품과 재고 생성을 같은 서비스 흐름에서 처리한다. 중복 상품 수량은 먼저 합산하고 repository가 정한 순서로 재고를 잠근다.
- 재고 변경은 `Inventory.deduct`·`restore`를 사용한다. 결제 후 주문 생성과 재고 차감은 `happygallery-payment-flows`의 confirm 처리와 맞춘다.
- 독립적으로 수정하는 `Product`의 `@Version`을 유지해 관리자 동시 수정의 덮어쓰기를 막는다.
- 재고 상품과 주문 제작 상품의 재고·처리 규칙을 구분한다. 필요한 필드만 읽는 조회는 projection을 검토한다.
- 비밀 Q&A는 로그인한 작성자 소유권으로 보호한다. 공용 비밀번호·공개 확인 API를 추가하지 않는다. 공개·회원·관리자 조회의 노출 범위를 함께 확인한다.

## 검증

- 재고 규칙은 해당 policy test, 상품·재고 흐름은 `ProductInventoryUseCaseIT`를 실행한다.
- 주문과 재고 처리도 바뀌면 `ConcurrentOrderUseCaseIT` 또는 `OrderApprovalUseCaseIT`를 추가한다. HTTP 변경은 `api-contract`를 적용한다.
