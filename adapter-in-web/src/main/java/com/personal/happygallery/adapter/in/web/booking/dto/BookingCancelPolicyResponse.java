package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.time.TimeBoundary;
import java.time.Clock;
import java.time.LocalDateTime;

public record BookingCancelPolicyResponse(
        boolean refundable,
        LocalDateTime deadlineAt,
        boolean passCreditRestorable,
        String warningCode
) {
    private static final String PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE =
            "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE";

    public static BookingCancelPolicyResponse from(Booking booking, Clock clock) {
        LocalDateTime deadlineAt = TimeBoundary.refundDeadlineAt(booking.getSlot().getStartAt());
        boolean booked = booking.getStatus() == BookingStatus.BOOKED;
        boolean refundable = booked && TimeBoundary.isRefundable(booking.getSlot().getStartAt(), clock);
        boolean passCreditRestorable = booking.isPassBooking() && refundable;
        String warningCode = resolveWarningCode(booking, booked, passCreditRestorable);

        return new BookingCancelPolicyResponse(
                refundable,
                deadlineAt,
                passCreditRestorable,
                warningCode);
    }

    private static String resolveWarningCode(Booking booking, boolean booked, boolean passCreditRestorable) {
        if (booked && booking.isPassBooking() && !passCreditRestorable) {
            return PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE;
        }
        return null;
    }
}
