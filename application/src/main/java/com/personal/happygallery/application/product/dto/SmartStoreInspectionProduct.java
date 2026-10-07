package com.personal.happygallery.application.product.dto;

/** 스마트스토어 검수·판매 제한 상품 항목이다. */
public record SmartStoreInspectionProduct(
        Long channelProductNo,
        String reason,
        String action,
        boolean restorationRequestAvailable
) {}
