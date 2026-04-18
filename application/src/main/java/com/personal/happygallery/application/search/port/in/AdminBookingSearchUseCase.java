package com.personal.happygallery.application.search.port.in;

import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;

public interface AdminBookingSearchUseCase {

    OffsetPage<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                              String keyword, int page, int size);
}
