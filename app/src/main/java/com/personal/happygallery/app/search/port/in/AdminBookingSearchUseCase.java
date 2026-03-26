package com.personal.happygallery.app.search.port.in;

import com.personal.happygallery.app.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.app.web.OffsetPage;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;

public interface AdminBookingSearchUseCase {

    OffsetPage<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                              String keyword, int page, int size);
}
