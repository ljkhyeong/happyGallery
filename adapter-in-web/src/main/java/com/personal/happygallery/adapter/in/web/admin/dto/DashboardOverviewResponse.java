package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.DashboardOverview;
import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardOverviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long todayRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int todayOrderCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pendingApprovalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int todayBookingCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long monthRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int monthOrderCount
) {

    public static DashboardOverviewResponse from(DashboardOverview overview) {
        return new DashboardOverviewResponse(
                overview.todayRevenue(),
                overview.todayOrderCount(),
                overview.pendingApprovalCount(),
                overview.todayBookingCount(),
                overview.monthRevenue(),
                overview.monthOrderCount()
        );
    }
}
