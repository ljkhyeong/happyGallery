package com.personal.happygallery.infra.dashboard.adapter;

import com.personal.happygallery.app.dashboard.dto.DashboardOverview;
import com.personal.happygallery.app.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.app.dashboard.dto.RefundStats;
import com.personal.happygallery.app.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.app.dashboard.dto.StatusCount;
import com.personal.happygallery.app.dashboard.dto.TopProduct;
import com.personal.happygallery.domain.time.Clocks;
import com.personal.happygallery.infra.dashboard.mapper.SalesStatsMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class MyBatisSalesStatsAdapterTest {

    @Test
    @DisplayName("개요 조회는 주입된 Clock 기준 오늘 서울 시간대 범위를 사용한다")
    void findOverviewUsesInjectedClockForTodayRange() {
        RecordingSalesStatsMapper mapper = new RecordingSalesStatsMapper();
        Clock fixedClock = Clock.fixed(
                ZonedDateTime.of(2026, 3, 5, 10, 0, 0, 0, Clocks.SEOUL).toInstant(),
                Clocks.SEOUL);
        MyBatisSalesStatsAdapter adapter = new MyBatisSalesStatsAdapter(fixedClock, mapper);

        adapter.findOverview(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertSoftly(softly -> {
            softly.assertThat(mapper.todayFrom).isEqualTo(LocalDateTime.of(2026, 3, 4, 15, 0));
            softly.assertThat(mapper.todayTo).isEqualTo(LocalDateTime.of(2026, 3, 5, 15, 0));
            softly.assertThat(mapper.rangeFrom).isEqualTo(LocalDateTime.of(2026, 2, 29, 15, 0));
            softly.assertThat(mapper.rangeTo).isEqualTo(LocalDateTime.of(2026, 3, 31, 15, 0));
        });
    }

    private static final class RecordingSalesStatsMapper implements SalesStatsMapper {
        private LocalDateTime todayFrom;
        private LocalDateTime todayTo;
        private LocalDateTime rangeFrom;
        private LocalDateTime rangeTo;

        @Override
        public DashboardOverview findOverview(LocalDateTime todayFrom, LocalDateTime todayTo,
                                              LocalDateTime rangeFrom, LocalDateTime rangeTo) {
            this.todayFrom = todayFrom;
            this.todayTo = todayTo;
            this.rangeFrom = rangeFrom;
            this.rangeTo = rangeTo;
            return new DashboardOverview(0L, 0, 0, 0, 0L, 0);
        }

        @Override
        public List<PeriodSalesSummary> findSalesSummary(LocalDateTime rangeFrom, LocalDateTime rangeTo,
                                                         String granularity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RevenueBreakdown findRevenueBreakdown(LocalDateTime rangeFrom, LocalDateTime rangeTo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<StatusCount> findOrderStatusDistribution() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RefundStats findRefundStats(LocalDateTime rangeFrom, LocalDateTime rangeTo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TopProduct> findTopProducts(LocalDateTime rangeFrom, LocalDateTime rangeTo,
                                                int limit, String sort) {
            throw new UnsupportedOperationException();
        }
    }
}
