package com.personal.happygallery.app.dashboard.dto;

public record RefundStats(
        int totalRefundCount,
        long totalRefundedAmount,
        double refundRate
) {}
