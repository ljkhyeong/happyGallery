package com.personal.happygallery.application.dashboard.dto;

public record DashboardOverview(
        long todayRevenue,
        int todayOrderCount,
        int pendingApprovalCount,
        int todayBookingCount,
        long monthRevenue,
        int monthOrderCount
) {}
