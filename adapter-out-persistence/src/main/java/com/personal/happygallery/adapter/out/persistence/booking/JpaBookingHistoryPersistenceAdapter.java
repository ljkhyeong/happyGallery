package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.domain.booking.BookingHistory;
import org.springframework.stereotype.Repository;

@Repository
class JpaBookingHistoryPersistenceAdapter implements BookingHistoryPort {

    private final BookingHistoryRepository repository;

    JpaBookingHistoryPersistenceAdapter(BookingHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookingHistory save(BookingHistory history) {
        return repository.save(history);
    }

    @Override
    public long countByBookingId(Long bookingId) {
        return repository.countByBookingId(bookingId);
    }
}
