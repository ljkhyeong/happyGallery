package com.personal.happygallery.application.product.dto;

/** 스마트스토어 원상품 목록 항목이다. */
public record SmartStoreCatalogProduct(
        Long originProductNo,
        Long channelProductNo,
        String name,
        String status,
        long salePrice,
        Integer stockQuantity,
        String imageUrl
) {}
