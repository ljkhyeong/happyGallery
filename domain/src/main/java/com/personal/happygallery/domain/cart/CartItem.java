package com.personal.happygallery.domain.cart;

import com.personal.happygallery.domain.crypto.Sha256;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

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

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "line_key", nullable = false, length = 64)
    private String lineKey;

    @Column(nullable = false)
    private int qty;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "cart_item_text_inputs",
            joinColumns = @JoinColumn(name = "cart_item_id"))
    @OrderBy("sortOrder ASC")
    private List<CartItemTextInput> textInputs = List.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CartItem() {}

    public CartItem(Long userId, Long productId, Long productVariantId,
                    List<CartItemTextInput> textInputs, int qty, LocalDateTime createdAt) {
        OrderAmountCalculator.requireQuantity(qty);
        this.userId = userId;
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.textInputs = List.copyOf(textInputs);
        this.lineKey = lineKey(productId, productVariantId, textInputs);
        this.qty = qty;
        this.createdAt = createdAt;
        this.updatedAt = this.createdAt;
    }

    /** 현재 수량에 더한 결과도 {@link OrderAmountCalculator#requireQuantity} 범위 안이어야 한다. */
    public void addQty(int delta, LocalDateTime updatedAt) {
        updateQty(this.qty + delta, updatedAt);
    }

    public void updateQty(int newQty, LocalDateTime updatedAt) {
        OrderAmountCalculator.requireQuantity(newQty);
        this.qty = newQty;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public List<CartItemTextInput> getTextInputs() { return List.copyOf(textInputs); }
    public int getQty() { return qty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static String lineKey(Long productId, Long productVariantId,
                                 List<CartItemTextInput> textInputs) {
        StringBuilder canonical = new StringBuilder()
                .append("product=").append(productId)
                .append("|variant=").append(productVariantId == null ? 0 : productVariantId)
                .append("|inputs=");
        textInputs.stream()
                .sorted(Comparator.comparing(CartItemTextInput::getOptionKey))
                .forEach(input -> canonical
                        .append(input.getOptionKey()).append('=')
                        .append(Base64.getEncoder().encodeToString(
                                input.getValue().getBytes(StandardCharsets.UTF_8))).append(';'));
        return Sha256.hex(canonical.toString());
    }
}
