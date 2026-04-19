package com.personal.happygallery.application.dashboard.dto;

public record RevenueBreakdown(
        long orderRevenue,
        long bookingDepositRevenue,
        long bookingBalanceRevenue,
        long passPurchaseRevenue,
        long totalRevenue
) {}
