package com.personal.happygallery.app.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;

public record BookingResponse(
        Long bookingId,
        String bookingNumber,
        String accessToken,
        Long slotId,
        String status,
        long depositAmount,
        long balanceAmount,
        String className
) {
    public static BookingResponse from(Booking booking, String rawAccessToken) {
        return new BookingResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                rawAccessToken,
                booking.getSlot().getId(),
                booking.getStatus().name(),
                booking.getDepositAmount(),
                booking.getBalanceAmount(),
                booking.getBookingClass().getName()
        );
    }
}
