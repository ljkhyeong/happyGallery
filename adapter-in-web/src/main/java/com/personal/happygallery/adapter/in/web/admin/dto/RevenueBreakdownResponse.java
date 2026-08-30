package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.RevenueBreakdown;
import io.swagger.v3.oas.annotations.media.Schema;

public record RevenueBreakdownResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long orderRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long bookingDepositRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long bookingBalanceRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long passPurchaseRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalRevenue
) {

    public static RevenueBreakdownResponse from(RevenueBreakdown breakdown) {
        return new RevenueBreakdownResponse(
                breakdown.orderRevenue(),
                breakdown.bookingDepositRevenue(),
                breakdown.bookingBalanceRevenue(),
                breakdown.passPurchaseRevenue(),
                breakdown.totalRevenue()
        );
    }
}
