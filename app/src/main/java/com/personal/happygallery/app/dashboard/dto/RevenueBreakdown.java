package com.personal.happygallery.app.dashboard.dto;

public record RevenueBreakdown(
        long orderRevenue,
        long bookingDepositRevenue,
        long bookingBalanceRevenue,
        long passPurchaseRevenue,
        long totalRevenue
) {}
