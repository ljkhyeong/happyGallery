package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.booking.dto.BookingCancelPolicyResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Clock;
import java.time.LocalDateTime;

public record MyBookingDetail(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long classId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long slotId,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"})
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "8")
        int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"UNPAID", "PAID"})
        String balanceStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passBooking,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingCancelPolicyResponse cancelPolicy,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true)
        RefundProgressResponse refund
) {
    public static MyBookingDetail from(Booking b, Refund refund, Clock clock) {
        return new MyBookingDetail(b.getId(), b.getBookingClass().getId(), b.getSlot().getId(), b.getStatus().name(),
                b.getBookingClass().getName(),
                b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                b.getParticipantCount(),
                b.getDepositAmount(), b.getBalanceAmount(),
                b.getBalanceStatus().name(), b.isPassBooking(),
                BookingCancelPolicyResponse.from(b, clock),
                refund != null ? RefundProgressResponse.from(refund) : null);
    }
}
