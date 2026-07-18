package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;

public record RefundStatusResponse(Long refundId,
                                   long amount,
                                   RefundStatus status,
                                   int attemptCount,
                                   String failReason) {

    public static RefundStatusResponse from(Refund refund) {
        return new RefundStatusResponse(
                refund.getId(),
                refund.getAmount(),
                refund.getStatus(),
                refund.getAttemptCount(),
                refund.getFailReason());
    }
}
