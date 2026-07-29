package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record RefundStatusResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long refundId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int attemptCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String failReason
) {

    public static RefundStatusResponse from(Refund refund) {
        return new RefundStatusResponse(
                refund.getId(),
                refund.getAmount(),
                refund.getStatus(),
                refund.getAttemptCount(),
                refund.getFailReason());
    }
}
