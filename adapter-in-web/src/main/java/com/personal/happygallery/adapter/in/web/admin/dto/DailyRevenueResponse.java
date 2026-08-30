package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.DailyRevenue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DailyRevenueResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revenue
) {

    public static DailyRevenueResponse from(DailyRevenue dailyRevenue) {
        return new DailyRevenueResponse(dailyRevenue.date(), dailyRevenue.revenue());
    }
}
