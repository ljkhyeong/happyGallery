package com.personal.happygallery.domain.pass;

import com.personal.happygallery.domain.category.CategoryName;

/**
 * 구매 시점에 확정해 저장하는 8회권 상품 정책.
 *
 * <p>기존 상수의 적용 범위는 판매 후 계약이므로 변경하지 않고, 정책 변경 시 새 상수를 추가한다.
 */
public enum PassPlan {

    /** 정책 도입 전에 판매되어 모든 클래스에 사용할 수 있었던 이용권. 신규 판매에는 사용하지 않는다. */
    LEGACY_ALL_CLASSES("전체 클래스 8회권"),

    /** 향수 원데이 클래스를 제외한 정규 공예 클래스용 8회권. */
    REGULAR_CRAFT_8("정규 공예 8회권");

    private final String displayName;

    PassPlan(String displayName) {
        this.displayName = displayName;
    }

    public boolean supportsClass(String category, boolean passEligible) {
        String normalizedCategory = CategoryName.required(category);
        return this == LEGACY_ALL_CLASSES
                || (passEligible && !"PERFUME".equals(normalizedCategory));
    }

    public String getDisplayName() {
        return displayName;
    }
}
