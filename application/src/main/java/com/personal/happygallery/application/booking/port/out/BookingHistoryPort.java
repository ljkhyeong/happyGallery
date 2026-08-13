package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingHistory;

public interface BookingHistoryPort {

    <S extends BookingHistory> S save(S history);

    long countByBookingId(Long bookingId);
}
