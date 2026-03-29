package com.personal.happygallery.infra.dashboard.adapter;

import com.personal.happygallery.app.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.app.search.port.out.AdminBookingSearchPort;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.infra.dashboard.mapper.AdminBookingSearchMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 예약 검색 어댑터.
 * slots.start_at은 UTC 변환 없이 서울 시간대 기준 날짜 범위를 직접 사용한다.
 * (기존 {@code DefaultAdminBookingQueryService}와 동일한 패턴)
 */
@Component
class MyBatisAdminBookingSearchAdapter implements AdminBookingSearchPort {

    private final AdminBookingSearchMapper mapper;

    MyBatisAdminBookingSearchAdapter(AdminBookingSearchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                               String keyword, int offset, int size) {
        return mapper.search(
                status != null ? status.name() : null,
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? dateTo.plusDays(1).atStartOfDay() : null,
                keyword, offset, size);
    }

    @Override
    public long count(BookingStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword) {
        return mapper.count(
                status != null ? status.name() : null,
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? dateTo.plusDays(1).atStartOfDay() : null,
                keyword);
    }
}
