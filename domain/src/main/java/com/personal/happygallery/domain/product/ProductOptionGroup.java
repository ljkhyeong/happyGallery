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

@Entity
@Table(name = "product_option_groups")
public class ProductOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_key", nullable = false, length = ProductOptionPolicy.MAX_KEY_LENGTH)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 10)
    private ProductOptionType type;

    @Column(nullable = false, length = ProductOptionPolicy.MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "input_placeholder", length = ProductOptionPolicy.MAX_PLACEHOLDER_LENGTH)
    private String inputPlaceholder;

    @Column(name = "input_max_length")
    private Integer inputMaxLength;

    @Column(name = "input_price_adjustment")
    private Long inputPriceAdjustment;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ProductOptionGroup() {}

    public ProductOptionGroup(Long productId, String key, ProductOptionType type, String name,
                              boolean required, int sortOrder, String inputPlaceholder,
                              Integer inputMaxLength, Long inputPriceAdjustment) {
        if (productId == null || type == null) {
            throw new IllegalArgumentException("상품과 옵션 유형은 필수입니다.");
        }
        this.productId = productId;
        this.key = ProductOptionPolicy.requireKey(key, "옵션");
        this.type = type;
        apply(name, required, sortOrder, inputPlaceholder, inputMaxLength, inputPriceAdjustment);
        this.active = true;
    }

    public void update(String name, boolean required, int sortOrder, String inputPlaceholder,
                       Integer inputMaxLength, Long inputPriceAdjustment) {
        apply(name, required, sortOrder, inputPlaceholder, inputMaxLength, inputPriceAdjustment);
        this.active = true;
    }

    private void apply(String name, boolean required, int sortOrder, String inputPlaceholder,
                       Integer inputMaxLength, Long inputPriceAdjustment) {
        this.name = ProductOptionPolicy.requireName(name, "옵션명");
        this.required = required;
        this.sortOrder = ProductOptionPolicy.requireSortOrder(sortOrder);
        if (type == ProductOptionType.SELECT) {
            if (inputPlaceholder != null || inputMaxLength != null || inputPriceAdjustment != null) {
                throw new IllegalArgumentException("선택형 옵션에는 직접입력 설정을 둘 수 없습니다.");
            }
            this.inputPlaceholder = null;
            this.inputMaxLength = null;
            this.inputPriceAdjustment = null;
            return;
        }
        if (inputMaxLength == null
                || inputMaxLength < 1
                || inputMaxLength > ProductOptionPolicy.MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException(
                    "직접입력형 옵션 최대 글자 수는 1자 이상 "
                            + ProductOptionPolicy.MAX_INPUT_LENGTH + "자 이하여야 합니다.");
        }
        this.inputPlaceholder = ProductOptionPolicy.optionalText(
                inputPlaceholder, "직접입력 안내", ProductOptionPolicy.MAX_PLACEHOLDER_LENGTH);
        this.inputMaxLength = inputMaxLength;
        this.inputPriceAdjustment = ProductOptionPolicy.requireTextPriceAdjustment(
                inputPriceAdjustment == null ? 0L : inputPriceAdjustment);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getKey() { return key; }
    public ProductOptionType getType() { return type; }
    public String getName() { return name; }
    public boolean isRequired() { return required; }
    public int getSortOrder() { return sortOrder; }
    public String getInputPlaceholder() { return inputPlaceholder; }
    public Integer getInputMaxLength() { return inputMaxLength; }
    public Long getInputPriceAdjustment() { return inputPriceAdjustment; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
