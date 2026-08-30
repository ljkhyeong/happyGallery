package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.booking.dto.BookingCancelPolicyResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase.BookingDetail;
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
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
        RefundProgressResponse refund,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String receiptUrl
) {
    public static MyBookingDetail from(BookingDetail detail, Clock clock) {
        Booking b = detail.booking();
        return new MyBookingDetail(b.getId(), b.getBookingClass().getId(), b.getSlot().getId(), b.getStatus().name(),
                b.getBookingClass().getName(),
                b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                b.getParticipantCount(),
                b.getDepositAmount(), b.getBalanceAmount(),
                b.getBalanceStatus().name(), b.isPassBooking(),
                BookingCancelPolicyResponse.from(b, clock),
                detail.refund() != null ? RefundProgressResponse.from(detail.refund()) : null,
                detail.receiptUrl());
    }
}
