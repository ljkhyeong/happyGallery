package com.personal.happygallery.app.booking.port.out;

import com.personal.happygallery.domain.booking.BookingHistory;

public interface BookingHistoryPort {

    BookingHistory save(BookingHistory history);

    long countByBookingId(Long bookingId);
}
