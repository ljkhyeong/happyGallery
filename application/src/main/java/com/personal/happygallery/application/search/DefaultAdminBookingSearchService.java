package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.search.port.out.AdminBookingSearchPort;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminBookingSearchService implements AdminBookingSearchUseCase {

    private final AdminBookingSearchPort searchPort;

    DefaultAdminBookingSearchService(AdminBookingSearchPort searchPort) {
        this.searchPort = searchPort;
    }

    @Override
    public OffsetPage<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                     String keyword, int page, int size) {
        return AdminSearchHelper.search(searchPort, status, dateFrom, dateTo, keyword, page, size);
    }
}
