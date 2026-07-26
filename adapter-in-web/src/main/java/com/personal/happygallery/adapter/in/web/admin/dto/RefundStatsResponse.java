package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.RefundStats;
import io.swagger.v3.oas.annotations.media.Schema;

public record RefundStatsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalRefundCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalRefundedAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) double refundRate
) {

    public static RefundStatsResponse from(RefundStats stats) {
        return new RefundStatsResponse(
                stats.totalRefundCount(),
                stats.totalRefundedAmount(),
                stats.refundRate()
        );
    }
}
