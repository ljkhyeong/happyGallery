package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.domain.payment.RefundStatus;

public record PassRefundResponse(int canceledBookings,
                                 int refundCredits,
                                 long refundAmount,
                                 Long refundId,
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
