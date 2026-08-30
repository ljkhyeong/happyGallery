package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import com.personal.happygallery.domain.order.OrderClaimType;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record OrderClaimResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderClaimType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderClaimResolution requestedResolution,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderClaimStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String customerReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String adminNote,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long resolvedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long completedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String replacementCarrier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String replacementTrackingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long maximumRefundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long refundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) RefundStatus refundStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime requestedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime resolvedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime completedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ClaimedItemResponse> items
) {
    public static OrderClaimResponse from(OrderClaimView view) {
        return new OrderClaimResponse(
                view.id(),
                view.orderId(),
                view.type(),
                view.requestedResolution(),
                view.status(),
                view.customerReason(),
                view.adminNote(),
                view.resolvedByAdminId(),
                view.completedByAdminId(),
                view.replacementCarrier(),
                view.replacementTrackingNumber(),
                view.maximumRefundAmount(),
                view.refundAmount(),
                view.refundStatus(),
                view.requestedAt(),
                view.resolvedAt(),
                view.completedAt(),
                view.items().stream().map(ClaimedItemResponse::from).toList());
    }

    public static List<OrderClaimResponse> fromAll(List<OrderClaimView> views) {
        return views.stream().map(OrderClaimResponse::from).toList();
    }

    public record ClaimedItemResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderItemId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long unitPrice
    ) {
        private static ClaimedItemResponse from(OrderClaimView.Item item) {
            return new ClaimedItemResponse(
                    item.orderItemId(),
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    item.unitPrice());
        }
    }
}
