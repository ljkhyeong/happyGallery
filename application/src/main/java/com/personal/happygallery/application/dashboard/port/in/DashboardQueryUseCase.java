package com.personal.happygallery.application.dashboard.port.in;

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
import java.time.LocalDate;
import java.util.List;

public interface DashboardQueryUseCase {

    DashboardOverview getOverview(LocalDate from, LocalDate to);

    List<PeriodSalesSummary> getSalesSummary(LocalDate from, LocalDate to, Granularity granularity);

    RevenueBreakdown getRevenueBreakdown(LocalDate from, LocalDate to);

    List<StatusCount> getOrderStatusDistribution();

    RefundStats getRefundStats(LocalDate from, LocalDate to);

    List<TopProduct> getTopProducts(LocalDate from, LocalDate to, int limit, TopProductSort sort);

    List<DailyRevenue> getDailyRevenueSeries(LocalDate from, LocalDate to);

    List<SlotUtilization> getSlotUtilization(LocalDate from, LocalDate to);
}
