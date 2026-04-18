package com.personal.happygallery.application.search.port.out;

import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
import java.util.List;

public interface AdminBookingSearchPort extends AdminSearchPort<BookingStatus, AdminBookingSearchRow> {
}
