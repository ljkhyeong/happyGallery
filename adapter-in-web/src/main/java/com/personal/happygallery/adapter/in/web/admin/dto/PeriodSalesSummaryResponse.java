package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.PeriodSalesSummary;
import io.swagger.v3.oas.annotations.media.Schema;

public record PeriodSalesSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String periodLabel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int orderCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long avgOrderValue
) {

    public static PeriodSalesSummaryResponse from(PeriodSalesSummary summary) {
        return new PeriodSalesSummaryResponse(
                summary.periodLabel(),
                summary.totalRevenue(),
                summary.orderCount(),
                summary.avgOrderValue()
        );
    }
}
