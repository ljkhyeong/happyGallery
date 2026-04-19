package com.personal.happygallery.application.product;

import com.personal.happygallery.domain.product.ProductType;

/**
 * 상품 목록 필터 조건.
 *
 * <p>모든 필드가 null이면 전체 ACTIVE 상품을 반환한다.
 *
 * @param type     상품 유형 필터 (null = 미필터)
 * @param category 카테고리 필터 (null = 미필터)
 * @param keyword  상품명 LIKE 검색 (null = 미필터, max 100자)
 * @param sort     정렬 기준 (non-null, 기본 NEWEST)
 */
public record ProductFilter(
        ProductType type,
        String category,
        String keyword,
        ProductSortOrder sort
) {
    public enum ProductSortOrder {
        NEWEST, PRICE_ASC, PRICE_DESC;

        /** 쿼리 파라미터 문자열을 enum으로 변환한다. 매칭 실패 시 NEWEST. */
        public static ProductSortOrder fromParam(String param) {
            if (param == null) return NEWEST;
            return switch (param) {
                case "price_asc" -> PRICE_ASC;
                case "price_desc" -> PRICE_DESC;
                default -> NEWEST;
            };
        }
    }

    public static ProductFilter defaults() {
        return new ProductFilter(null, null, null, ProductSortOrder.NEWEST);
    }

    /** 모든 필터가 기본값(미필터 + 최신순)인지 확인한다. */
    public boolean isDefault() {
        return type == null && category == null && keyword == null && sort == ProductSortOrder.NEWEST;
    }
}
