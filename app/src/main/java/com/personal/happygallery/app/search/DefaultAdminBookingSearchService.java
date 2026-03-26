package com.personal.happygallery.app.search;

import com.personal.happygallery.app.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.app.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.app.search.port.out.AdminBookingSearchPort;
import com.personal.happygallery.app.web.OffsetPage;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminBookingSearchService implements AdminBookingSearchUseCase {

    private final AdminBookingSearchPort searchPort;

    DefaultAdminBookingSearchService(AdminBookingSearchPort searchPort) {
        this.searchPort = searchPort;
    }

    private static final int MAX_SIZE = 100;

    @Override
    public OffsetPage<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                     String keyword, int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        long totalCount = searchPort.count(status, dateFrom, dateTo, keyword);
        if (totalCount == 0 || (long) page * clampedSize >= totalCount) {
            return OffsetPage.of(List.of(), page, clampedSize, totalCount);
        }
        List<AdminBookingSearchRow> rows = searchPort.search(
                status, dateFrom, dateTo, keyword, page * clampedSize, clampedSize);
        return OffsetPage.of(rows, page, clampedSize, totalCount);
    }
}
