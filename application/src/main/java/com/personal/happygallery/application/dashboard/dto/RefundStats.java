package com.personal.happygallery.application.dashboard.dto;

public record RefundStats(
        int totalRefundCount,
        long totalRefundedAmount,
        double refundRate
) {}
