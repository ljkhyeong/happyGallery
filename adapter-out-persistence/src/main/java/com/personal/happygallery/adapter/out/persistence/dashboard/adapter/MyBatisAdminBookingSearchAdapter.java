package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.adapter.out.persistence.dashboard.mapper.AdminBookingSearchMapper;
import com.personal.happygallery.adapter.out.persistence.time.SeoulDateTimeRangeConverter;
import com.personal.happygallery.application.search.port.out.AdminBookingSearchPort;
import com.personal.happygallery.application.search.port.out.AdminBookingSearchResult;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.user.PersonalName;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 예약 검색 어댑터.
 * slots.start_at은 UTC 변환 없이 서울 시간대 기준 날짜 범위를 직접 사용한다.
 * (기존 {@code DefaultAdminBookingQueryService}와 동일한 패턴)
 */
@Component
class MyBatisAdminBookingSearchAdapter implements AdminBookingSearchPort {

    private static final Pattern FORMATTED_BOOKING_ID = Pattern.compile(
            "^BK-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final AdminBookingSearchMapper mapper;
    private final BlindIndexer blindIndexer;

    MyBatisAdminBookingSearchAdapter(AdminBookingSearchMapper mapper, BlindIndexer blindIndexer) {
        this.mapper = mapper;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public List<AdminBookingSearchResult> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                  String keyword, int offset, int size) {
        AdminSearchKeyword searchKeyword = AdminSearchKeyword.parse(keyword, FORMATTED_BOOKING_ID);
        return mapper.search(
                status != null ? status.name() : null,
                dateFrom != null ? SeoulDateTimeRangeConverter.toLocalStart(dateFrom) : null,
                dateTo != null ? SeoulDateTimeRangeConverter.toLocalExclusiveEnd(dateTo) : null,
                searchKeyword.keyword(), indexKeyword(searchKeyword.keyword()),
                searchKeyword.exactId(), offset, size);
    }

    @Override
    public long count(BookingStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword) {
        AdminSearchKeyword searchKeyword = AdminSearchKeyword.parse(keyword, FORMATTED_BOOKING_ID);
        return mapper.count(
                status != null ? status.name() : null,
                dateFrom != null ? SeoulDateTimeRangeConverter.toLocalStart(dateFrom) : null,
                dateTo != null ? SeoulDateTimeRangeConverter.toLocalExclusiveEnd(dateTo) : null,
                searchKeyword.keyword(), indexKeyword(searchKeyword.keyword()), searchKeyword.exactId());
    }

    private String indexKeyword(String keyword) {
        return keyword == null ? null : blindIndexer.index(PersonalName.required(keyword));
    }
}
