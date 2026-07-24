package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import com.personal.happygallery.domain.order.OrderClaimType;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderClaimView(
        Long id,
        Long orderId,
        OrderClaimType type,
        OrderClaimResolution requestedResolution,
        OrderClaimStatus status,
        String customerReason,
        String adminNote,
        Long resolvedByAdminId,
        Long completedByAdminId,
        String replacementCarrier,
        String replacementTrackingNumber,
        long maximumRefundAmount,
        Long refundAmount,
        RefundStatus refundStatus,
        LocalDateTime requestedAt,
        LocalDateTime resolvedAt,
        LocalDateTime completedAt,
        List<Item> items
) {
    public record Item(
            Long orderItemId,
            Long productId,
            String productName,
            int quantity,
            long unitPrice
    ) {}
}
