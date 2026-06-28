package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Refund;
import java.time.LocalDateTime;

public record FailedRefundResponse(
        Long refundId,
        Long bookingId,
        Long orderId,
        Long passPurchaseId,
        long amount,
        String failReason,
        LocalDateTime createdAt
) {

    public static FailedRefundResponse from(Refund refund) {
        return new FailedRefundResponse(
                refund.getId(),
                refund.getBookingId(),
                refund.getOrderId(),
                refund.getPassPurchaseId(),
                refund.getAmount(),
                refund.getFailReason() != null ? refund.getFailReason() : "",
                refund.getCreatedAt()
        );
    }
}
