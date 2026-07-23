package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelResult;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminBookingCancelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "CANCELED") String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passCreditRestored,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositRefundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) RefundStatus depositRefundStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean balanceSettlementRequired,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean manualCompensationRequired
) {
    public static AdminBookingCancelResponse from(AdminCancelResult result) {
        return new AdminBookingCancelResponse(
                result.booking().getId(),
                result.booking().getStatus().name(),
                result.passCreditRestored(),
                result.refund() != null ? result.refund().getAmount() : 0,
                result.refund() != null ? result.refund().getStatus() : null,
                result.balanceSettlementRequired(),
                result.booking().isPassBooking() && !result.passCreditRestored());
    }
}
