package com.personal.happygallery.app.search.port.out;

import com.personal.happygallery.app.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
import java.util.List;

public interface AdminBookingSearchPort {

    List<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                        String keyword, int offset, int size);

    long count(BookingStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword);
}
