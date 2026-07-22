package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelResult;
import com.personal.happygallery.domain.payment.RefundStatus;

public record AdminBookingCancelResponse(
        Long bookingId,
        String status,
        boolean passCreditRestored,
        long depositRefundAmount,
        RefundStatus depositRefundStatus,
        boolean balanceSettlementRequired,
        boolean manualCompensationRequired
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
