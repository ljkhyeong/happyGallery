package com.personal.happygallery.application.search.dto;

import java.time.LocalDateTime;

public record AdminBookingSearchRow(
        Long bookingId,
        String bookingNumber,
        String bookerType,
        String bookerName,
        String bookerPhone,
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
