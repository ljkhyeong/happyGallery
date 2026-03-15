package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import org.springframework.stereotype.Component;

/**
 * {@link BookingHistoryRepository}(infra) → {@link BookingHistoryPort}(app) 브릿지 어댑터.
 */
@Component
class BookingHistoryPortAdapter implements BookingHistoryPort {

    private final BookingHistoryRepository bookingHistoryRepository;

    BookingHistoryPortAdapter(BookingHistoryRepository bookingHistoryRepository) {
        this.bookingHistoryRepository = bookingHistoryRepository;
    }

    @Override
    public BookingHistory save(BookingHistory history) {
        return bookingHistoryRepository.save(history);
    }

    @Override
    public long countByBookingId(Long bookingId) {
        return bookingHistoryRepository.countByBookingId(bookingId);
    }
}
