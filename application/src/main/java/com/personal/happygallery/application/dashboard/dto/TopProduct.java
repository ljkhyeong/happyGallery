package com.personal.happygallery.application.dashboard.dto;

public record TopProduct(
        Long productId,
        String productName,
        String productType,
        long totalRevenue,
        int totalQuantity
) {}
