package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.booking.Refund;
import java.time.LocalDateTime;

public record FailedRefundResponse(
        Long refundId,
        Long bookingId,
        Long orderId,
        long amount,
        String failReason,
        LocalDateTime createdAt
) {

    public static FailedRefundResponse from(Refund refund) {
        return new FailedRefundResponse(
                refund.getId(),
                refund.getBooking() != null ? refund.getBooking().getId() : null,
                refund.getOrderId(),
                refund.getAmount(),
                refund.getFailReason() != null ? refund.getFailReason() : "",
                refund.getCreatedAt()
        );
    }
}
