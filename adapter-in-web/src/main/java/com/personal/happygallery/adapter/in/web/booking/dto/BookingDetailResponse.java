package com.personal.happygallery.adapter.in.web.booking.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskPhoneMiddle;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Clock;
import java.time.LocalDateTime;

public record BookingDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long classId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long slotId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"})
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String guestName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String guestPhone,  // 마스킹: 010****5678
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingCancelPolicyResponse cancelPolicy,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true)
        RefundProgressResponse refund
) {
    public static BookingDetailResponse from(Booking booking,
                                             Refund refund,
                                             String guestName,
                                             String guestPhone,
                                             Clock clock) {
        String maskedPhone = maskPhoneMiddle(guestPhone);
        return new BookingDetailResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                booking.getBookingClass().getId(),
                booking.getSlot().getId(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getBookingClass().getName(),
                booking.getStatus().name(),
                booking.getParticipantCount(),
                booking.getDepositAmount(),
                booking.getBalanceAmount(),
                guestName,
                maskedPhone,
                BookingCancelPolicyResponse.from(booking, clock),
                refund != null ? RefundProgressResponse.from(refund) : null
        );
    }
}
