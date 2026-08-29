package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "smartstore_product_orders")
public class SmartStoreProductOrder {

    @Id
    @Column(name = "product_order_id", length = 30)
    private String productOrderId;

    @Column(name = "order_id", nullable = false, length = 30)
    private String orderId;

    @Column(name = "origin_product_no", nullable = false)
    private Long originProductNo;

    @Column(name = "item_no")
    private Long itemNo;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "product_name", nullable = false, length = 4000)
    private String productName;

    @Column(name = "product_option", length = 4000)
    private String productOption;

    @Column(name = "product_order_status", nullable = false, length = 40)
    private String productOrderStatus;

    @Column(name = "claim_type", length = 40)
    private String claimType;

    @Column(name = "claim_status", length = 40)
    private String claimStatus;

    @Column(name = "initial_quantity", nullable = false)
    private int initialQuantity;

    @Column(name = "remain_quantity", nullable = false)
    private int remainQuantity;

    @Column(name = "inventory_applied_quantity", nullable = false)
    private int inventoryAppliedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "attention_reason", length = 30)
    private SmartStoreOrderAttentionReason attentionReason;

    @Column(name = "last_changed_type", nullable = false, length = 40)
    private String lastChangedType;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "last_changed_at", nullable = false)
    private LocalDateTime lastChangedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected SmartStoreProductOrder() {}

    public SmartStoreProductOrder(
            String productOrderId,
            String orderId,
            Long originProductNo,
            Long itemNo,
            String productName,
            String productOption,
            String productOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity,
            String lastChangedType,
            LocalDateTime paymentDate,
            LocalDateTime lastChangedAt) {
        this.productOrderId = requireText(productOrderId, "상품 주문 번호");
        this.inventoryAppliedQuantity = 0;
        refresh(orderId, originProductNo, itemNo, productName, productOption,
                productOrderStatus, claimType, claimStatus, initialQuantity, remainQuantity,
                lastChangedType, paymentDate, lastChangedAt);
    }

    public boolean refresh(
            String orderId,
            Long originProductNo,
            Long itemNo,
            String productName,
            String productOption,
            String productOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity,
            String lastChangedType,
            LocalDateTime paymentDate,
            LocalDateTime changedAt) {
        if (lastChangedAt != null
                && (changedAt.isBefore(lastChangedAt)
                || changedAt.equals(lastChangedAt) && Objects.equals(this.lastChangedType, lastChangedType))) {
            return false;
        }
        if (originProductNo == null || initialQuantity < 0 || remainQuantity < 0) {
            throw new IllegalArgumentException("스마트스토어 상품과 주문 수량이 올바르지 않습니다.");
        }
        this.orderId = requireText(orderId, "주문 번호");
        this.originProductNo = originProductNo;
        this.itemNo = itemNo;
        this.productName = requireText(productName, "상품명");
        this.productOption = productOption;
        this.productOrderStatus = requireText(productOrderStatus, "상품 주문 상태");
        this.claimType = claimType;
        this.claimStatus = claimStatus;
        this.initialQuantity = initialQuantity;
        this.remainQuantity = remainQuantity;
        this.lastChangedType = requireText(lastChangedType, "최종 변경 구분");
        this.paymentDate = paymentDate;
        this.lastChangedAt = Objects.requireNonNull(changedAt, "최종 변경 일시는 필수입니다.");
        return true;
    }

    public void mapTo(Long productId, Long productVariantId) {
        this.productId = Objects.requireNonNull(productId, "내부 상품은 필수입니다.");
        this.productVariantId = productVariantId;
    }

    public void applyInventoryQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("적용 재고 수량은 0 이상이어야 합니다.");
        }
        this.inventoryAppliedQuantity = quantity;
        this.attentionReason = null;
    }

    public void requireAttention(SmartStoreOrderAttentionReason reason) {
        this.attentionReason = Objects.requireNonNull(reason, "확인 사유는 필수입니다.");
    }

    public void resolveAttention() {
        this.attentionReason = null;
    }

    public boolean hasMapping() {
        return productId != null;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }

    public String getProductOrderId() { return productOrderId; }
    public String getOrderId() { return orderId; }
    public Long getOriginProductNo() { return originProductNo; }
    public Long getItemNo() { return itemNo; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public String getProductName() { return productName; }
    public String getProductOption() { return productOption; }
    public String getProductOrderStatus() { return productOrderStatus; }
    public String getClaimType() { return claimType; }
    public String getClaimStatus() { return claimStatus; }
    public int getInitialQuantity() { return initialQuantity; }
    public int getRemainQuantity() { return remainQuantity; }
    public int getInventoryAppliedQuantity() { return inventoryAppliedQuantity; }
    public SmartStoreOrderAttentionReason getAttentionReason() { return attentionReason; }
    public String getLastChangedType() { return lastChangedType; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public LocalDateTime getLastChangedAt() { return lastChangedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
