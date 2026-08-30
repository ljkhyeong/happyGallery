package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.product.ProductOptionPolicy;
import com.personal.happygallery.domain.product.ProductOptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class OrderOptionSnapshot {

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 10)
    private ProductOptionType type;

    @Column(name = "group_name", nullable = false, length = ProductOptionPolicy.MAX_NAME_LENGTH)
    private String groupName;

    @Column(nullable = false, length = ProductOptionPolicy.MAX_INPUT_LENGTH)
    private String value;

    @Column(name = "price_adjustment", nullable = false)
    private long priceAdjustment;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected OrderOptionSnapshot() {}

    public OrderOptionSnapshot(ProductOptionType type, String groupName, String value,
                               long priceAdjustment, int sortOrder) {
        if (type == null || priceAdjustment < 0) {
            throw new IllegalArgumentException("주문 옵션 스냅샷이 올바르지 않습니다.");
        }
        this.type = type;
        this.groupName = ProductOptionPolicy.requireName(groupName, "주문 옵션명");
        this.value = ProductOptionPolicy.requireText(
                value, "주문 옵션값", ProductOptionPolicy.MAX_INPUT_LENGTH);
        this.priceAdjustment = priceAdjustment;
        this.sortOrder = ProductOptionPolicy.requireSortOrder(sortOrder);
    }

    public ProductOptionType getType() { return type; }
    public String getGroupName() { return groupName; }
    public String getValue() { return value; }
    public long getPriceAdjustment() { return priceAdjustment; }
    public int getSortOrder() { return sortOrder; }
}
