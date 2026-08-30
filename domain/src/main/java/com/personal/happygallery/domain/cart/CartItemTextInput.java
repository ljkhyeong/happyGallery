package com.personal.happygallery.domain.cart;

import com.personal.happygallery.domain.product.ProductOptionPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CartItemTextInput {

    @Column(name = "option_group_id", nullable = false)
    private Long optionGroupId;

    @Column(name = "option_key", nullable = false, length = ProductOptionPolicy.MAX_KEY_LENGTH)
    private String optionKey;

    @Column(nullable = false, length = ProductOptionPolicy.MAX_INPUT_LENGTH)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CartItemTextInput() {}

    public CartItemTextInput(Long optionGroupId, String optionKey, String value, int sortOrder) {
        if (optionGroupId == null) {
            throw new IllegalArgumentException("직접입력형 옵션 그룹은 필수입니다.");
        }
        this.optionGroupId = optionGroupId;
        this.optionKey = ProductOptionPolicy.requireKey(optionKey, "직접입력형 옵션");
        this.value = ProductOptionPolicy.requireText(
                value, "직접입력형 옵션값", ProductOptionPolicy.MAX_INPUT_LENGTH);
        this.sortOrder = ProductOptionPolicy.requireSortOrder(sortOrder);
    }

    public Long getOptionGroupId() { return optionGroupId; }
    public String getOptionKey() { return optionKey; }
    public String getValue() { return value; }
    public int getSortOrder() { return sortOrder; }
}
