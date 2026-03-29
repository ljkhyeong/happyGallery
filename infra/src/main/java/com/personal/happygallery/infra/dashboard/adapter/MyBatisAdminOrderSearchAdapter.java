package com.personal.happygallery.infra.dashboard.adapter;

import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.app.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.common.time.SeoulDateTimeRangeConverter;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.dashboard.mapper.AdminOrderSearchMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 서울 시간대 날짜 범위를 UTC {@link LocalDateTime}으로 변환한 뒤 MyBatis 매퍼에 전달한다.
 */
@Component
class MyBatisAdminOrderSearchAdapter implements AdminOrderSearchPort {

    private final AdminOrderSearchMapper mapper;

    MyBatisAdminOrderSearchAdapter(AdminOrderSearchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                             String keyword, int offset, int size) {
        return mapper.search(
                status != null ? status.name() : null,
                dateFrom != null ? SeoulDateTimeRangeConverter.toUtcStart(dateFrom) : null,
                dateTo != null ? SeoulDateTimeRangeConverter.toUtcExclusiveEnd(dateTo) : null,
                keyword, offset, size);
    }

    @Override
    public long count(OrderStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword) {
        return mapper.count(
                status != null ? status.name() : null,
                dateFrom != null ? SeoulDateTimeRangeConverter.toUtcStart(dateFrom) : null,
                dateTo != null ? SeoulDateTimeRangeConverter.toUtcExclusiveEnd(dateTo) : null,
                keyword);
    }
}
