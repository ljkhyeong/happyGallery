package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;

public record RescheduleResponse(
        Long bookingId,
        String bookingNumber,
        Long slotId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String className,
        String status
) {
    public static RescheduleResponse from(Booking booking) {
        return new RescheduleResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                booking.getSlot().getId(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getBookingClass().getName(),
                booking.getStatus().name()
        );
    }
}
