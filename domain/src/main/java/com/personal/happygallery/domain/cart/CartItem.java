package com.personal.happygallery.domain.cart;

import com.personal.happygallery.domain.order.OrderAmountCalculator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 회원 장바구니 항목 — cart_items 테이블. */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int qty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CartItem() {}

    public CartItem(Long userId, Long productId, int qty, LocalDateTime createdAt) {
        OrderAmountCalculator.requireQuantity(qty);
        this.userId = userId;
        this.productId = productId;
        this.qty = qty;
        this.createdAt = createdAt;
        this.updatedAt = this.createdAt;
    }

    public void addQty(int delta, LocalDateTime updatedAt) {
        try {
            updateQty(Math.addExact(this.qty, delta), updatedAt);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("장바구니 수량이 너무 큽니다.", e);
        }
    }

    public void updateQty(int newQty, LocalDateTime updatedAt) {
        OrderAmountCalculator.requireQuantity(newQty);
        this.qty = newQty;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public int getQty() { return qty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
