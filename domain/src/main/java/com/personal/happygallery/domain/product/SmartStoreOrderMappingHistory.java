package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "smartstore_order_mapping_history")
public class SmartStoreOrderMappingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "origin_product_no", nullable = false)
    private Long originProductNo;

    @Column(name = "option_id")
    private Long optionId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "closed_at", nullable = false, updatable = false)
    private LocalDateTime closedAt;

    protected SmartStoreOrderMappingHistory() {}

    public SmartStoreOrderMappingHistory(SmartStoreStockMapping mapping, LocalDateTime closedAt) {
        this.productId = mapping.getProductId();
        this.productVariantId = mapping.getProductVariantId();
        this.originProductNo = mapping.getOriginProductNo();
        this.optionId = mapping.getOptionId();
        this.enabled = mapping.isEnabled();
        this.closedAt = closedAt;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public Long getOriginProductNo() { return originProductNo; }
    public Long getOptionId() { return optionId; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
