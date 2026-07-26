package com.personal.happygallery.application.search.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
        String source,
        int participantCount,
        long depositAmount,
        LocalDateTime depositPaidAt,
        long balanceAmount,
        String balanceStatus,
        LocalDateTime balancePaidAt,
        boolean arrears,
        boolean passBooking,
        OffsetDateTime createdAt
) {
}
