package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductVariantSelection {

    @Column(name = "option_group_id", nullable = false)
    private Long optionGroupId;

    @Column(name = "option_value_id", nullable = false)
    private Long optionValueId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductVariantSelection() {}

    public ProductVariantSelection(Long optionGroupId, Long optionValueId, int sortOrder) {
        if (optionGroupId == null || optionValueId == null) {
            throw new IllegalArgumentException("조합 옵션 그룹과 값은 필수입니다.");
        }
        this.optionGroupId = optionGroupId;
        this.optionValueId = optionValueId;
        this.sortOrder = ProductOptionPolicy.requireSortOrder(sortOrder);
    }

    public Long getOptionGroupId() { return optionGroupId; }
    public Long getOptionValueId() { return optionValueId; }
    public int getSortOrder() { return sortOrder; }
}
