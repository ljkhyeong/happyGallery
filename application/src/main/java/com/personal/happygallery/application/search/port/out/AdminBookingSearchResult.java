package com.personal.happygallery.application.search.port.out;

import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.BookingSource;
import com.personal.happygallery.domain.booking.BookingStatus;
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
        LocalDateTime createdAt
) {
}
