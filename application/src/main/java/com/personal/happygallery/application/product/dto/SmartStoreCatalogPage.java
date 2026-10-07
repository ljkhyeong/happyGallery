package com.personal.happygallery.application.product.dto;

import java.util.List;

/** 스마트스토어 원상품 목록 한 페이지다. */
public record SmartStoreCatalogPage(
        List<SmartStoreCatalogProduct> products,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public SmartStoreCatalogPage {
        products = products == null ? List.of() : List.copyOf(products);
    }
}
