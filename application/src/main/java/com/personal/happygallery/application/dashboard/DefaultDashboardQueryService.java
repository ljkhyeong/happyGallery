package com.personal.happygallery.application.dashboard;

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
import com.personal.happygallery.application.dashboard.port.out.BookingStatsQueryPort;
import com.personal.happygallery.application.dashboard.port.out.SalesStatsQueryPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultDashboardQueryService implements DashboardQueryUseCase {

    private static final int MAX_RANGE_DAYS = 365;
    private static final int MAX_LIMIT = 100;

    private final SalesStatsQueryPort salesStatsQueryPort;
    private final BookingStatsQueryPort bookingStatsQueryPort;

    public DefaultDashboardQueryService(SalesStatsQueryPort salesStatsQueryPort,
                                        BookingStatsQueryPort bookingStatsQueryPort) {
        this.salesStatsQueryPort = salesStatsQueryPort;
        this.bookingStatsQueryPort = bookingStatsQueryPort;
    }

    @Override
    public DashboardOverview getOverview(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return salesStatsQueryPort.findOverview(from, to);
    }

    @Override
    public List<PeriodSalesSummary> getSalesSummary(LocalDate from, LocalDate to, Granularity granularity) {
        validateRange(from, to);
        return salesStatsQueryPort.findSalesSummary(from, to, granularity);
    }

    @Override
    public RevenueBreakdown getRevenueBreakdown(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return salesStatsQueryPort.findRevenueBreakdown(from, to);
    }

    @Override
    public List<StatusCount> getOrderStatusDistribution() {
        return salesStatsQueryPort.findOrderStatusDistribution();
    }

    @Override
    public RefundStats getRefundStats(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return salesStatsQueryPort.findRefundStats(from, to);
    }

    @Override
    public List<TopProduct> getTopProducts(LocalDate from, LocalDate to, int limit, TopProductSort sort) {
        validateRange(from, to);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "limit은 1~" + MAX_LIMIT + " 범위여야 합니다.");
        }
        return salesStatsQueryPort.findTopProducts(from, to, limit, sort);
    }

    @Override
    public List<DailyRevenue> getDailyRevenueSeries(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return salesStatsQueryPort.findDailyRevenueSeries(from, to);
    }

    @Override
    public List<SlotUtilization> getSlotUtilization(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return bookingStatsQueryPort.findSlotUtilization(from, to);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "from은 to보다 이전이어야 합니다: from=" + from + ", to=" + to);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "조회 범위는 최대 " + MAX_RANGE_DAYS + "일입니다.");
        }
    }
}
