package com.personal.happygallery.application.product.dto;

/** 스마트스토어 원상품의 옵션 조합이다. */
public record SmartStoreChannelOption(
        Long optionId,
        String name,
        int stockQuantity,
        long price,
        boolean usable
) {}
