package com.personal.happygallery.application.product.dto;

import java.util.List;

/** 스마트스토어 검수·판매 제한 상품 목록 한 페이지다. */
public record SmartStoreInspectionPage(
        List<SmartStoreInspectionProduct> products,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public SmartStoreInspectionPage {
        products = products == null ? List.of() : List.copyOf(products);
    }
}
