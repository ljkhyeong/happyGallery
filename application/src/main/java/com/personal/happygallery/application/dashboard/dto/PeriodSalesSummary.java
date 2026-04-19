package com.personal.happygallery.application.dashboard.dto;

public record PeriodSalesSummary(
        String periodLabel,
        long totalRevenue,
        int orderCount,
        long avgOrderValue
) {}
