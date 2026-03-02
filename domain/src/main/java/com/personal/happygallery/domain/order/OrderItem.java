package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 주문 상품 라인 — order_items 테이블 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int qty;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    protected OrderItem() {}

    /**
     * 주문 상품 라인 생성.
     *
     * @param order     소속 주문
     * @param productId 상품 ID
     * @param qty       수량
     * @param unitPrice 단가 (원)
     */
    public OrderItem(Order order, Long productId, int qty, long unitPrice) {
        this.order = order;
        this.productId = productId;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getProductId() { return productId; }
    public int getQty() { return qty; }
    public long getUnitPrice() { return unitPrice; }
}
