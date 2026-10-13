package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestOrderSummary;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.order.OrderStatus;
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
    public static GuestRecoveredOrderPageResponse from(CursorPage<GuestOrderSummary> page) {
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
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
    ) {
        public static OrderSummary from(GuestOrderSummary order) {
            return new OrderSummary(order.orderId(), order.status(), order.totalAmount(), order.createdAt());
        }
    }
}
