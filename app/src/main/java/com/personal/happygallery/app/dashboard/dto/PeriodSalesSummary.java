package com.personal.happygallery.app.dashboard.dto;

public record PeriodSalesSummary(
        String periodLabel,
        long totalRevenue,
        int orderCount,
        long avgOrderValue
) {}
