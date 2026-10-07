package com.personal.happygallery.domain.pass;

import com.personal.happygallery.domain.category.CategoryName;

/**
 * 구매 시점에 확정해 저장하는 이용권 상품 정책.
 *
 * <p>판매한 계획의 횟수와 적용 범위는 구매 계약이므로, 정책을 바꿀 때는 새 상수를 추가한다.
 */
public enum PassPlan {

    /** 향수 원데이 클래스를 제외한 정규 공예 클래스용 4회권. */
    REGULAR_CRAFT_4("정규 공예 4회권", 4);

    private final String displayName;
    private final int totalCredits;

    PassPlan(String displayName, int totalCredits) {
        this.displayName = displayName;
        this.totalCredits = totalCredits;
    }

    public boolean supportsClass(String category, boolean passEligible) {
        return passEligible && !"PERFUME".equals(CategoryName.required(category));
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTotalCredits() {
        return totalCredits;
    }
}
