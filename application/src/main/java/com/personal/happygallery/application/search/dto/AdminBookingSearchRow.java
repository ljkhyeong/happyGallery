package com.personal.happygallery.application.search.dto;

import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.BookingSource;
import com.personal.happygallery.domain.booking.BookingStatus;
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
        BookingStatus status,
        BookingSource source,
        int participantCount,
        long depositAmount,
        LocalDateTime depositPaidAt,
        long balanceAmount,
        BalanceStatus balanceStatus,
        LocalDateTime balancePaidAt,
        boolean arrears,
        boolean passBooking,
        OffsetDateTime createdAt
) {
}
