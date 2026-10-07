package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredOrder;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

public record GuestRecoveredOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<OrderSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static GuestRecoveredOrderPageResponse from(CursorPage<RecoveredOrder> page) {
        return new GuestRecoveredOrderPageResponse(
                page.content().stream()
                        .map(OrderSummary::from)
                        .toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @Schema(name = "GuestRecordRecoveryOrderSummary")
    public record OrderSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {
                            "PAID_APPROVAL_PENDING", "APPROVED_FULFILLMENT_PENDING",
                            "REJECTED", "CUSTOMER_CANCELED", "AUTO_REFUND_TIMEOUT",
                            "IN_PRODUCTION", "DELAY_CONSENT_PENDING", "DELAY_ACCEPTED",
                            "DELAY_REJECTED_CANCELED", "SHIPPING_PREPARING", "SHIPPED",
                            "DELIVERED", "PICKUP_READY", "PICKED_UP", "PICKUP_EXPIRED",
                            "PICKUP_FORFEITED", "COMPLETED"
                    })
            String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
    ) {
        public static OrderSummary from(RecoveredOrder order) {
            return new OrderSummary(order.orderId(), order.status(), order.totalAmount(), order.createdAt());
        }
    }
}
