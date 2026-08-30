package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 관리자 수동 재고 조정 이력. */
@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InventoryAdjustmentType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "quantity_before", nullable = false)
    private int quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "adjusted_by_admin_id")
    private Long adjustedByAdminId;

    @Column(name = "adjusted_by", nullable = false, length = 100)
    private String adjustedBy;

    @Column(name = "adjusted_at", nullable = false, updatable = false)
    private LocalDateTime adjustedAt;

    protected InventoryAdjustment() {}

    public InventoryAdjustment(Long productId,
                               Long productVariantId,
                               InventoryAdjustmentType type,
                               int quantity,
                               int quantityBefore,
                               int quantityAfter,
                               String reason,
                               Long adjustedByAdminId,
                               String adjustedBy,
                               LocalDateTime adjustedAt) {
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.type = type;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.reason = reason.strip();
        this.adjustedByAdminId = adjustedByAdminId;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt;
    }

    public InventoryAdjustment(Long productId,
                               InventoryAdjustmentType type,
                               int quantity,
                               int quantityBefore,
                               int quantityAfter,
                               String reason,
                               Long adjustedByAdminId,
                               String adjustedBy,
                               LocalDateTime adjustedAt) {
        this(productId, null, type, quantity, quantityBefore, quantityAfter,
                reason, adjustedByAdminId, adjustedBy, adjustedAt);
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public InventoryAdjustmentType getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getQuantityBefore() { return quantityBefore; }
    public int getQuantityAfter() { return quantityAfter; }
    public String getReason() { return reason; }
    public Long getAdjustedByAdminId() { return adjustedByAdminId; }
    public String getAdjustedBy() { return adjustedBy; }
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
}
