package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long bookingId,
        String bookingNumber,
        String guestName,
        String guestPhone,
        String className,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        long depositAmount,
        long balanceAmount,
        boolean passBooking
) {

    public static AdminBookingResponse from(Booking booking) {
        return new AdminBookingResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                booking.getGuest().getName(),
                booking.getGuest().getPhone(),
                booking.getBookingClass().getName(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getStatus().name(),
                booking.getDepositAmount(),
                booking.getBalanceAmount(),
                booking.isPassBooking()
        );
    }
}
