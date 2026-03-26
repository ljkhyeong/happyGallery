package com.personal.happygallery.infra.dashboard.mapper;

import com.personal.happygallery.app.dashboard.dto.DashboardOverview;
import com.personal.happygallery.app.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.app.dashboard.dto.RefundStats;
import com.personal.happygallery.app.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.app.dashboard.dto.StatusCount;
import com.personal.happygallery.app.dashboard.dto.TopProduct;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SalesStatsMapper {

    DashboardOverview findOverview(@Param("todayFrom") LocalDateTime todayFrom,
                                  @Param("todayTo") LocalDateTime todayTo,
                                  @Param("rangeFrom") LocalDateTime rangeFrom,
                                  @Param("rangeTo") LocalDateTime rangeTo);

    List<PeriodSalesSummary> findSalesSummary(@Param("rangeFrom") LocalDateTime rangeFrom,
                                              @Param("rangeTo") LocalDateTime rangeTo,
                                              @Param("granularity") String granularity);

    RevenueBreakdown findRevenueBreakdown(@Param("rangeFrom") LocalDateTime rangeFrom,
                                          @Param("rangeTo") LocalDateTime rangeTo);

    List<StatusCount> findOrderStatusDistribution();

    RefundStats findRefundStats(@Param("rangeFrom") LocalDateTime rangeFrom,
                                @Param("rangeTo") LocalDateTime rangeTo);

    List<TopProduct> findTopProducts(@Param("rangeFrom") LocalDateTime rangeFrom,
                                     @Param("rangeTo") LocalDateTime rangeTo,
                                     @Param("limit") int limit,
                                     @Param("sort") String sort);
}
