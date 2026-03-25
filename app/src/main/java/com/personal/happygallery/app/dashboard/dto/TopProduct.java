package com.personal.happygallery.app.dashboard.dto;

public record TopProduct(
        Long productId,
        String productName,
        String productType,
        long totalRevenue,
        int totalQuantity
) {}
