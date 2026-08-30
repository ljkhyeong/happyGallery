package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase.ParticipantReductionResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReduceBookingParticipantsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "BOOKED") String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int canceledParticipantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long depositAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long refundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) RefundProgressResponse refund
) {
    public static ReduceBookingParticipantsResponse from(ParticipantReductionResult result) {
        return new ReduceBookingParticipantsResponse(
                result.booking().getId(),
                result.booking().getStatus().name(),
                result.booking().getParticipantCount(),
                result.canceledParticipantCount(),
                result.booking().getDepositAmount(),
                result.booking().getBalanceAmount(),
                result.refund() == null ? 0L : result.refund().getAmount(),
                result.refund() == null ? null : RefundProgressResponse.from(result.refund()));
    }
}
