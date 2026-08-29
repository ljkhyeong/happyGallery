package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "smartstore_stock_mappings")
public class SmartStoreStockMapping {

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

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected SmartStoreStockMapping() {}

    public SmartStoreStockMapping(
            Long productId,
            Long productVariantId,
            Long originProductNo,
            Long optionId,
            boolean enabled) {
        if (productId == null || originProductNo == null || originProductNo < 1) {
            throw new IllegalArgumentException("상품과 스마트스토어 원상품 번호는 필수입니다.");
        }
        if ((productVariantId == null) != (optionId == null)) {
            throw new IllegalArgumentException("옵션 조합과 스마트스토어 옵션 번호를 함께 입력해 주세요.");
        }
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.originProductNo = originProductNo;
        this.optionId = optionId;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public Long getOriginProductNo() { return originProductNo; }
    public Long getOptionId() { return optionId; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
