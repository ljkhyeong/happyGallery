package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberPassRefundResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int canceledBookings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int refundCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long refundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        RefundStatus refundStatus) {

    public static MemberPassRefundResponse from(PassRefundUseCase.PassRefundResult result) {
        return new MemberPassRefundResponse(
                result.canceledBookings(),
                result.refundCredits(),
                result.refundAmount(),
                result.refundStatus());
    }
}
