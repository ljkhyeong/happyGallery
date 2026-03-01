package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * 상품 재고 — inventory 테이블.
 *
 * <p>product_id가 PK이자 FK. 상품 1개당 재고 row 1개를 유지한다.
 * 단일 작품(수량=1) 중복 판매 방지: {@code deduct()} 호출 전 반드시
 * {@link com.personal.happygallery.infra.product.InventoryRepository#findByProductIdWithLock}으로
 * 비관적 락을 획득해야 한다.
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Inventory() {}

    /**
     * 재고 생성. 단일 작품은 quantity=1로 생성한다.
     *
     * @param product  연결 상품
     * @param quantity 초기 수량
     */
    public Inventory(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    /**
     * 재고를 차감한다.
     * 재고 부족 시 {@link com.personal.happygallery.common.error.InventoryNotEnoughException}.
     *
     * @param qty 차감 수량
     */
    public void deduct(int qty) {
        InventoryPolicy.checkSufficient(this.quantity, qty);
        this.quantity -= qty;
    }

    /**
     * 재고를 복구한다. 주문 거절/환불 시 호출한다.
     *
     * @param qty 복구 수량
     */
    public void restore(int qty) {
        this.quantity += qty;
    }

    /** 재고가 1개 이상 남아 있으면 true. */
    public boolean isAvailable() {
        return quantity > 0;
    }

    public Long getProductId() { return productId; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public long getVersion() { return version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
