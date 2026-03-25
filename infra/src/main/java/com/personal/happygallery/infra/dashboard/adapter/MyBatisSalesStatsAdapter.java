package com.personal.happygallery.infra.dashboard.adapter;

import com.personal.happygallery.app.dashboard.dto.DailyRevenue;
import com.personal.happygallery.app.dashboard.dto.DashboardOverview;
import com.personal.happygallery.app.dashboard.dto.Granularity;
import com.personal.happygallery.app.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.app.dashboard.dto.RefundStats;
import com.personal.happygallery.app.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.app.dashboard.dto.StatusCount;
import com.personal.happygallery.app.dashboard.dto.TopProduct;
import com.personal.happygallery.app.dashboard.dto.TopProductSort;
import com.personal.happygallery.app.dashboard.port.out.SalesStatsQueryPort;
import com.personal.happygallery.infra.dashboard.mapper.SalesStatsMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * KST 날짜 범위를 UTC {@link LocalDateTime}으로 변환한 뒤 MyBatis 매퍼에 전달한다.
 * WHERE 절에서 인덱스를 탈 수 있도록 sargable 조건을 보장한다.
 */
@Component
class MyBatisSalesStatsAdapter implements SalesStatsQueryPort {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SalesStatsMapper salesStatsMapper;

    MyBatisSalesStatsAdapter(SalesStatsMapper salesStatsMapper) {
        this.salesStatsMapper = salesStatsMapper;
    }

    @Override
    public DashboardOverview findOverview(LocalDate from, LocalDate to) {
        LocalDate todayKst = ZonedDateTime.now(KST).toLocalDate();
        return salesStatsMapper.findOverview(
                toUtcStart(todayKst), toUtcEnd(todayKst),
                toUtcStart(from), toUtcEnd(to));
    }

    @Override
    public List<PeriodSalesSummary> findSalesSummary(LocalDate from, LocalDate to, Granularity granularity) {
        return salesStatsMapper.findSalesSummary(toUtcStart(from), toUtcEnd(to), granularity.name());
    }

    @Override
    public RevenueBreakdown findRevenueBreakdown(LocalDate from, LocalDate to) {
        return salesStatsMapper.findRevenueBreakdown(toUtcStart(from), toUtcEnd(to));
    }

    @Override
    public List<StatusCount> findOrderStatusDistribution() {
        return salesStatsMapper.findOrderStatusDistribution();
    }

    @Override
    public RefundStats findRefundStats(LocalDate from, LocalDate to) {
        return salesStatsMapper.findRefundStats(toUtcStart(from), toUtcEnd(to));
    }

    @Override
    public List<TopProduct> findTopProducts(LocalDate from, LocalDate to, int limit, TopProductSort sort) {
        return salesStatsMapper.findTopProducts(toUtcStart(from), toUtcEnd(to), limit, sort.name());
    }

    @Override
    public List<DailyRevenue> findDailyRevenueSeries(LocalDate from, LocalDate to) {
        return salesStatsMapper.findSalesSummary(toUtcStart(from), toUtcEnd(to), Granularity.DAILY.name())
                .stream()
                .map(s -> new DailyRevenue(LocalDate.parse(s.periodLabel()), s.totalRevenue()))
                .toList();
    }

    /** KST 날짜의 자정(00:00)을 UTC LocalDateTime으로 변환 */
    private static LocalDateTime toUtcStart(LocalDate kstDate) {
        return kstDate.atStartOfDay(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** KST 날짜의 익일 자정(다음날 00:00)을 UTC LocalDateTime으로 변환 — 반개구간 [start, end) */
    private static LocalDateTime toUtcEnd(LocalDate kstDate) {
        return kstDate.plusDays(1).atStartOfDay(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
