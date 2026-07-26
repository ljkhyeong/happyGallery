package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.time.TimeBoundary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Clock;
import java.time.LocalDateTime;

public record BookingCancelPolicyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean cancellable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean refundable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime deadlineAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passCreditRestorable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean manualCompensationRequired,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String warningCode
) {
    private static final String PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE =
            "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE";

    public static BookingCancelPolicyResponse from(Booking booking, Clock clock) {
        LocalDateTime deadlineAt = TimeBoundary.refundDeadlineAt(booking.getSlot().getStartAt());
        boolean cancellable = booking.isCustomerCancellationAllowed();
        boolean booked = booking.getStatus() == BookingStatus.BOOKED;
        boolean refundable = cancellable && TimeBoundary.isRefundable(booking.getSlot().getStartAt(), clock);
        boolean passCreditRestorable = booking.isPassBooking() && refundable;
        boolean manualCompensationRequired =
                refundable && booking.requiresManualDepositCompensation();
        boolean passCreditNotRestorable = booked && booking.isPassBooking() && !passCreditRestorable;
        String warningCode = passCreditNotRestorable
                ? PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE
                : null;

        return new BookingCancelPolicyResponse(
                cancellable,
                refundable,
                deadlineAt,
                passCreditRestorable,
                manualCompensationRequired,
                warningCode);
    }
}
