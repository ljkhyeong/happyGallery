package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record MyOrderSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime paidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt) {
    public static MyOrderSummary from(Order o) {
        return new MyOrderSummary(o.getId(), o.getStatus(),
                o.getTotalAmount(), o.getPaidAt(), o.getCreatedAt().atOffset(ZoneOffset.UTC));
    }

    public static List<MyOrderSummary> fromAll(List<Order> orders) {
        return orders.stream().map(MyOrderSummary::from).toList();
    }
}
