package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.application.dashboard.dto.DailyRevenue;
import com.personal.happygallery.application.dashboard.dto.DashboardOverview;
import com.personal.happygallery.application.dashboard.dto.Granularity;
import com.personal.happygallery.application.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.application.dashboard.dto.RefundStats;
import com.personal.happygallery.application.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.application.dashboard.dto.StatusCount;
import com.personal.happygallery.application.dashboard.dto.TopProduct;
import com.personal.happygallery.application.dashboard.dto.TopProductSort;
import com.personal.happygallery.application.dashboard.port.out.SalesAnalyticsPort;
import com.personal.happygallery.adapter.out.persistence.time.SeoulDateTimeRangeConverter;
import com.personal.happygallery.adapter.out.persistence.dashboard.mapper.SalesStatsMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 서울 현지시각 업무 열과 UTC DB 기본시각 열을 구분해 MyBatis 매퍼에 전달한다.
 * WHERE 절에서 인덱스를 탈 수 있도록 sargable 조건을 보장한다.
 */
@Component
class MyBatisSalesStatsAdapter implements SalesAnalyticsPort {

    private final Clock clock;
    private final SalesStatsMapper salesStatsMapper;

    MyBatisSalesStatsAdapter(Clock clock, SalesStatsMapper salesStatsMapper) {
        this.clock = clock;
        this.salesStatsMapper = salesStatsMapper;
    }

    @Override
    public DashboardOverview findOverview(LocalDate from, LocalDate to) {
        LocalDate todayInSeoul = LocalDate.now(clock);
        return salesStatsMapper.findOverview(
                SeoulDateTimeRangeConverter.toLocalStart(todayInSeoul),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(todayInSeoul),
                SeoulDateTimeRangeConverter.toUtcStart(todayInSeoul),
                SeoulDateTimeRangeConverter.toUtcExclusiveEnd(todayInSeoul),
                SeoulDateTimeRangeConverter.toLocalStart(from),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(to));
    }

    @Override
    public List<PeriodSalesSummary> findSalesSummary(LocalDate from, LocalDate to, Granularity granularity) {
        return salesStatsMapper.findSalesSummary(
                SeoulDateTimeRangeConverter.toLocalStart(from),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(to),
                granularity.name());
    }

    @Override
    public RevenueBreakdown findRevenueBreakdown(LocalDate from, LocalDate to) {
        return salesStatsMapper.findRevenueBreakdown(
                SeoulDateTimeRangeConverter.toLocalStart(from),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(to));
    }

    @Override
    public List<StatusCount> findOrderStatusDistribution() {
        return salesStatsMapper.findOrderStatusDistribution();
    }

    @Override
    public RefundStats findRefundStats(LocalDate from, LocalDate to) {
        return salesStatsMapper.findRefundStats(
                SeoulDateTimeRangeConverter.toLocalStart(from),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(to));
    }

    @Override
    public List<TopProduct> findTopProducts(LocalDate from, LocalDate to, int limit, TopProductSort sort) {
        return salesStatsMapper.findTopProducts(
                SeoulDateTimeRangeConverter.toLocalStart(from),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(to),
                limit,
                sort.name());
    }

    @Override
    public List<DailyRevenue> findDailyRevenueSeries(LocalDate from, LocalDate to) {
        return salesStatsMapper.findDailyRevenue(
                SeoulDateTimeRangeConverter.toLocalStart(from),
                SeoulDateTimeRangeConverter.toLocalExclusiveEnd(to));
    }
}
