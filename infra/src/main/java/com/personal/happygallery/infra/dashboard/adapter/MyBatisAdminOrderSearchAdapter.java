package com.personal.happygallery.infra.dashboard.adapter;

import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.app.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.dashboard.mapper.AdminOrderSearchMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * KST 날짜 범위를 UTC {@link LocalDateTime}으로 변환한 뒤 MyBatis 매퍼에 전달한다.
 */
@Component
class MyBatisAdminOrderSearchAdapter implements AdminOrderSearchPort {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdminOrderSearchMapper mapper;

    MyBatisAdminOrderSearchAdapter(AdminOrderSearchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                             String keyword, int offset, int size) {
        return mapper.search(
                status != null ? status.name() : null,
                dateFrom != null ? toUtcStart(dateFrom) : null,
                dateTo != null ? toUtcEnd(dateTo) : null,
                keyword, offset, size);
    }

    @Override
    public long count(OrderStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword) {
        return mapper.count(
                status != null ? status.name() : null,
                dateFrom != null ? toUtcStart(dateFrom) : null,
                dateTo != null ? toUtcEnd(dateTo) : null,
                keyword);
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
