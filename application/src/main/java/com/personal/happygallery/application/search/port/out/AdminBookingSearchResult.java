package com.personal.happygallery.application.search.port.out;

import java.time.LocalDateTime;

public record AdminBookingSearchResult(
        Long bookingId,
        String bookingNumber,
        String bookerType,
        String bookerNameEnc,
        String bookerPhoneEnc,
        String className,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        long depositAmount,
        long balanceAmount,
        boolean passBooking,
        LocalDateTime createdAt
) {
}
