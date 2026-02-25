package com.personal.happygallery.app.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;

public record CancelResponse(
        Long bookingId,
        String status,
        boolean refundable,
        long refundAmount
) {
    public static CancelResponse of(Booking booking, boolean refundable) {
        return new CancelResponse(
                booking.getId(),
                booking.getStatus().name(),
                refundable,
                refundable ? booking.getDepositAmount() : 0L
        );
    }
}
