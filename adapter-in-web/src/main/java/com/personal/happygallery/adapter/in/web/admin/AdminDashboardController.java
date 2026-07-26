package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.DailyRevenueResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.DashboardOverviewResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.PeriodSalesSummaryResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.RefundStatsResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.RevenueBreakdownResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SlotUtilizationResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.StatusCountResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.TopProductResponse;
import com.personal.happygallery.application.dashboard.dto.Granularity;
import com.personal.happygallery.application.dashboard.dto.TopProductSort;
import com.personal.happygallery.application.dashboard.port.in.DashboardQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final DashboardQueryUseCase dashboardQueryUseCase;

    public AdminDashboardController(DashboardQueryUseCase dashboardQueryUseCase) {
        this.dashboardQueryUseCase = dashboardQueryUseCase;
    }

    @GetMapping("/overview")
    @Operation(operationId = "overview")
    public DashboardOverviewResponse overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return DashboardOverviewResponse.from(dashboardQueryUseCase.getOverview(from, to));
    }

    @GetMapping("/sales-summary")
    @Operation(operationId = "salesSummary")
    public List<PeriodSalesSummaryResponse> salesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAILY") Granularity granularity) {
        return dashboardQueryUseCase.getSalesSummary(from, to, granularity).stream()
                .map(PeriodSalesSummaryResponse::from)
                .toList();
    }

    @GetMapping("/revenue-breakdown")
    @Operation(operationId = "revenueBreakdown")
    public RevenueBreakdownResponse revenueBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return RevenueBreakdownResponse.from(
                dashboardQueryUseCase.getRevenueBreakdown(from, to)
        );
    }

    @GetMapping("/order-status")
    @Operation(operationId = "orderStatusDistribution")
    public List<StatusCountResponse> orderStatusDistribution() {
        return dashboardQueryUseCase.getOrderStatusDistribution().stream()
                .map(StatusCountResponse::from)
                .toList();
    }

    @GetMapping("/refunds")
    @Operation(operationId = "refundStats")
    public RefundStatsResponse refundStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return RefundStatsResponse.from(dashboardQueryUseCase.getRefundStats(from, to));
    }

    @GetMapping("/top-products")
    @Operation(operationId = "topProducts")
    public List<TopProductResponse> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "REVENUE") TopProductSort sort) {
        return dashboardQueryUseCase.getTopProducts(from, to, limit, sort).stream()
                .map(TopProductResponse::from)
                .toList();
    }

    @GetMapping("/daily-revenue")
    @Operation(operationId = "dailyRevenue")
    public List<DailyRevenueResponse> dailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getDailyRevenueSeries(from, to).stream()
                .map(DailyRevenueResponse::from)
                .toList();
    }

    @GetMapping("/slot-utilization")
    @Operation(operationId = "slotUtilization")
    public List<SlotUtilizationResponse> slotUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getSlotUtilization(from, to).stream()
                .map(SlotUtilizationResponse::from)
                .toList();
    }
}
