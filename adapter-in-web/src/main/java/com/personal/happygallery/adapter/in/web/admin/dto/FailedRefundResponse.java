package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FailedRefundResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long refundId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long orderClaimId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long passPurchaseId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long paymentAttemptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"FAILED", "RETRYABLE", "RECONCILIATION_REQUIRED"})
        RefundStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int attemptCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String failReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
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
                refund.getStatus(),
                refund.getAttemptCount(),
                refund.getFailReason() != null ? refund.getFailReason() : "",
                refund.getCreatedAt()
        );
    }
}
