package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record PassRefundResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int canceledBookings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int refundCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long refundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long refundId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        RefundStatus refundStatus) {

    public static PassRefundResponse from(PassRefundUseCase.PassRefundResult result) {
        return new PassRefundResponse(
                result.canceledBookings(),
                result.refundCredits(),
                result.refundAmount(),
                result.refundId(),
                result.refundStatus());
    }
}
