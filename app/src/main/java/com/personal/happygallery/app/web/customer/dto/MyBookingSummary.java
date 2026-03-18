package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;

public record MyBookingSummary(Long bookingId, String status, String className,
                                LocalDateTime startAt, LocalDateTime endAt,
                                long depositAmount) {
    public static MyBookingSummary from(Booking b) {
        return new MyBookingSummary(b.getId(), b.getStatus().name(),
                b.getBookingClass().getName(),
                b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                b.getDepositAmount());
    }
}
