package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Refund;
import java.time.LocalDateTime;

public record FailedRefundResponse(
        Long refundId,
        Long bookingId,
        Long orderId,
        Long orderClaimId,
        Long passPurchaseId,
        Long paymentAttemptId,
        long amount,
        String status,
        int attemptCount,
        String failReason,
        LocalDateTime createdAt
) {

    public static FailedRefundResponse from(Refund refund) {
        return new FailedRefundResponse(
                refund.getId(),
                refund.getBookingId(),
                refund.getOrderId(),
                refund.getOrderClaimId(),
                refund.getPassPurchaseId(),
                refund.getPaymentAttemptId(),
                refund.getAmount(),
                refund.getStatus().name(),
                refund.getAttemptCount(),
                refund.getFailReason() != null ? refund.getFailReason() : "",
                refund.getCreatedAt()
        );
    }
}
