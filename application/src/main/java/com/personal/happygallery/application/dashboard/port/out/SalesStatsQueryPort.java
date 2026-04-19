package com.personal.happygallery.application.dashboard.port.out;

import com.personal.happygallery.application.dashboard.dto.DailyRevenue;
import com.personal.happygallery.application.dashboard.dto.DashboardOverview;
import com.personal.happygallery.application.dashboard.dto.Granularity;
import com.personal.happygallery.application.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.application.dashboard.dto.RefundStats;
import com.personal.happygallery.application.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.application.dashboard.dto.StatusCount;
import com.personal.happygallery.application.dashboard.dto.TopProduct;
import com.personal.happygallery.application.dashboard.dto.TopProductSort;
import java.time.LocalDate;
import java.util.List;

public interface SalesStatsQueryPort {

    DashboardOverview findOverview(LocalDate from, LocalDate to);

    List<PeriodSalesSummary> findSalesSummary(LocalDate from, LocalDate to, Granularity granularity);

    RevenueBreakdown findRevenueBreakdown(LocalDate from, LocalDate to);

    List<StatusCount> findOrderStatusDistribution();

    RefundStats findRefundStats(LocalDate from, LocalDate to);

    List<TopProduct> findTopProducts(LocalDate from, LocalDate to, int limit, TopProductSort sort);

    List<DailyRevenue> findDailyRevenueSeries(LocalDate from, LocalDate to);
}
