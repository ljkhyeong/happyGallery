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

### 2. 재고 차감 — 비관적 락 (`SELECT ... FOR UPDATE`)

```java
// InventoryRepository
@Lock(PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByProductIdWithLock(Long productId);
```

단일 작품 특성상 경합이 드물지만, 중복 판매 비용이 크므로 비관적 락 선택.
`SlotRepository.findByIdWithLock()` 패턴 재사용.

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

### 6. 장바구니 조회 — 읽기 전용 projection JOIN

- `CartItem`은 `productId`만 유지하고 `Product`, `Inventory` JPA 연관관계를 추가하지 않는다.
- `GET /api/v1/me/cart`는 `CartQueryPort`를 통해 장바구니 항목, 상품, 재고를 한 번의 projection JOIN 쿼리로 조회한다.
- 애플리케이션 서비스는 projection을 응답 모델로 변환하고 가용성과 합계만 계산한다.
- 상품별 개별 조회와 여러 조회 결과를 ID `Map`으로 다시 조립하지 않는다.

---

## 위험 포인트

- **목록 조회 N+1 위험**: 상품 목록은 재고를 `IN`으로 일괄 조회하고, 장바구니는 projection JOIN을 사용한다. 신규 목록 조회도 항목별 개별 조회를 만들지 않는다.
- **비관적 락 데드락**: 여러 상품을 순서 없이 잠글 경우 데드락 가능.
  → §8.2 주문 생성 시 product_id 오름차순 정렬 후 lock 획득 권장.
- **`restore()` 멱등성**: 환불 재시도 시 중복 복구 가능.
  → §8.2 환불 흐름에서 refund 상태 전이로 방어 필요.
