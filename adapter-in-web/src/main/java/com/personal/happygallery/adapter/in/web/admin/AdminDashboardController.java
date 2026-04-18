package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.dashboard.dto.DailyRevenue;
import com.personal.happygallery.application.dashboard.dto.DashboardOverview;
import com.personal.happygallery.application.dashboard.dto.Granularity;
import com.personal.happygallery.application.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.application.dashboard.dto.RefundStats;
import com.personal.happygallery.application.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import com.personal.happygallery.application.dashboard.dto.StatusCount;
import com.personal.happygallery.application.dashboard.dto.TopProduct;
import com.personal.happygallery.application.dashboard.dto.TopProductSort;
import com.personal.happygallery.application.dashboard.port.in.DashboardQueryUseCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/dashboard", "/admin/dashboard"})
public class AdminDashboardController {

    private final DashboardQueryUseCase dashboardQueryUseCase;

    public AdminDashboardController(DashboardQueryUseCase dashboardQueryUseCase) {
        this.dashboardQueryUseCase = dashboardQueryUseCase;
    }

    @GetMapping("/overview")
    public DashboardOverview overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getOverview(from, to);
    }

    @GetMapping("/sales-summary")
    public List<PeriodSalesSummary> salesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAILY") Granularity granularity) {
        return dashboardQueryUseCase.getSalesSummary(from, to, granularity);
    }

    @GetMapping("/revenue-breakdown")
    public RevenueBreakdown revenueBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getRevenueBreakdown(from, to);
    }

    @GetMapping("/order-status")
    public List<StatusCount> orderStatusDistribution() {
        return dashboardQueryUseCase.getOrderStatusDistribution();
    }

    @GetMapping("/refunds")
    public RefundStats refundStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getRefundStats(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProduct> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "REVENUE") TopProductSort sort) {
        return dashboardQueryUseCase.getTopProducts(from, to, limit, sort);
    }

    @GetMapping("/daily-revenue")
    public List<DailyRevenue> dailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getDailyRevenueSeries(from, to);
    }

    @GetMapping("/slot-utilization")
    public List<SlotUtilization> slotUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardQueryUseCase.getSlotUtilization(from, to);
    }
}
