package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.domain.payment.RefundStatus;

public record MemberPassRefundResponse(int canceledBookings,
                                       int refundCredits,
                                       long refundAmount,
                                       RefundStatus refundStatus) {

    public static MemberPassRefundResponse from(PassRefundUseCase.PassRefundResult result) {
        return new MemberPassRefundResponse(
                result.canceledBookings(),
                result.refundCredits(),
                result.refundAmount(),
                result.refundStatus());
    }
}
