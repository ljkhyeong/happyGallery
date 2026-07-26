package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

public record CancelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "CANCELED")
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "8")
        int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean refundable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long refundAmount,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true)
        RefundProgressResponse refund,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean manualCompensationRequired
) {
    public static CancelResponse from(BookingCancelUseCase.CancelResult result) {
        return new CancelResponse(
                result.booking().getId(),
                result.booking().getStatus().name(),
                result.booking().getParticipantCount(),
                result.refundable(),
                result.refund() != null ? result.refund().getAmount() : 0L,
                result.refund() != null ? RefundProgressResponse.from(result.refund()) : null,
                result.manualCompensationRequired()
        );
    }
}
