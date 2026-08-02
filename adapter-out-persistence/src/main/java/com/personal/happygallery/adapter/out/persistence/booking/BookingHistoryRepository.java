package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.domain.booking.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long> {

    /** 특정 예약의 이력 건수 — Proof 테스트용 */
    long countByBookingId(Long bookingId);
}
