# ADR-0012: 상품/재고 구현 결정 (§8.1)

**날짜**: 2026-03-02
**상태**: 승인됨

---

## 맥락

§8.1 상품/재고 구현. 단일 작품(quantity=1) 중심 온라인 쇼핑몰.
핵심 리스크: 동일 상품 동시 결제 → 중복 판매.

---

## 결정 사항

### 1. Inventory 엔티티 구조 — `@MapsId` 패턴

```java
@Entity @Table("inventory")
class Inventory {
    @Id @Column("product_id") Long productId;  // PK = FK
    @MapsId @OneToOne(LAZY) Product product;
    @Version long version;
    int quantity;
}
```

`product_id`가 PK이자 FK인 DDL(V2) 구조를 그대로 반영.
`@Version`은 잠재적 낙관적 락 전환을 위해 유지.

### 2. 재고 차감·복구 — productId 정렬 일괄 비관적 락

```java
// InventoryRepository
@Lock(PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
List<Inventory> findByProductIdInWithLock(List<Long> productIds);
```

단일 작품 특성상 경합이 드물지만, 중복 판매 비용이 크므로 비관적 락 선택.
`SlotRepository.findByIdWithLock()` 패턴 재사용.

한 주문에 여러 상품이 있으면 `InventoryService`가 중복 productId의 수량을 합산하고 productId 오름차순으로
모든 재고 row를 한 번에 잠근다. 상품별 잠금 조회 N회를 한 번의 `IN` 조회로 줄이면서 주문 간 잠금 순서를
고정해 교착 가능성도 낮춘다. 주문 거절·자동환불의 재고 복구도 같은 일괄 잠금 경로를 사용한다.

### 3. 정책 검증 — 기존 `InventoryPolicy` 재사용

`InventoryPolicy.checkSufficient(available, requested)` 이미 구현됨.
`Inventory.deduct()` 내부에서 호출 → 도메인 레이어에서 불변식 유지.

### 4. 재고 복구 메서드 (`restore()`) 선제 추가

§8.2 주문 거절/자동환불 시 복구가 필요함.
`InventoryService.restore()` 를 §8.1에서 함께 구현해 §8.2에서 바로 사용.

### 5. API 설계

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/admin/products` | 상품 등록 (name, type, price, quantity) → 201 |
| `GET`  | `/admin/products` | ACTIVE 상품 목록 |
| `GET`  | `/products/{id}` | 상품 상세 + `available` 필드 |

`available: true/false` — `inventory.quantity > 0` 여부를 응답에 포함.

직접 주문 prepare도 장바구니와 동일하게 `Product.status=ACTIVE`를 확인한다. 공개 목록에서 사라진 상품을
오래된 화면이나 조작 요청으로 직접 지정해도 재고 유무와 무관하게 결제 대상으로 확정하지 않는다.

### 6. 장바구니 조회 — 읽기 전용 projection JOIN

- `CartItem`은 `productId`만 유지하고 `Product`, `Inventory` JPA 연관관계를 추가하지 않는다.
- `GET /api/v1/me/cart`는 화면용 조합 조회를 나타내는 `CartReadModelPort`를 통해 장바구니 항목, 상품, 재고를 한 번의 projection JOIN 쿼리로 조회한다.
- 애플리케이션 서비스는 상품이 `ACTIVE`이고 재고가 장바구니 수량 이상일 때만 항목을 구매 가능으로 표시하며, 구매 가능한 항목 합계만 계산한다.
- 장바구니 결제 prepare도 같은 projection을 사용해 구매 가능한 항목을 선택하고 `cart_items.id`와 수량을 내부 결제 스냅샷에 보존한다. confirm 성공 시 같은 행을 ID 오름차순으로 잠금 조회해 스냅샷 수량만 차감한다.
- prepare 뒤 항목을 삭제하고 같은 상품을 다시 담으면 새 행 ID가 생기므로 과거 결제로 제거하지 않는다. 수량 변경과 결제 차감은 같은 장바구니 행 잠금에서 직렬화한다.
- 상품별 개별 조회와 여러 조회 결과를 ID `Map`으로 다시 조립하지 않는다.

---

## 위험 포인트

- **목록 조회 N+1 위험**: 상품 목록은 재고를 `IN`으로 일괄 조회하고, 장바구니는 projection JOIN을 사용한다. 신규 목록 조회도 항목별 개별 조회를 만들지 않는다.
- **비관적 락 데드락**: 여러 상품은 product_id 오름차순으로 한 번에 잠근다. 신규 다건 재고 변경도 `InventoryService.deductAll/restoreAll`을 사용한다.
- **`restore()` 멱등성**: 환불 재시도 시 중복 복구 가능.
  → §8.2 환불 흐름에서 refund 상태 전이로 방어 필요.
