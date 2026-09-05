package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.order.port.in.OrderQueryUseCase.OrderSummary;
import com.personal.happygallery.domain.order.OrderItem;
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MyOrderItemSummary> items) {
    public static MyOrderSummary from(OrderSummary view) {
        var o = view.order();
        return new MyOrderSummary(o.getId(), o.getStatus(),
                o.getTotalAmount(), o.getPaidAt(), o.getCreatedAt().atOffset(ZoneOffset.UTC),
                view.items().stream().map(MyOrderItemSummary::from).toList());
    }

    public record MyOrderItemSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderItemId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> options) {
        static MyOrderItemSummary from(OrderItem item) {
            return new MyOrderItemSummary(item.getId(), item.getProductName(), item.getQty(),
                    item.getOptionSnapshots().stream()
                            .map(option -> option.getGroupName() + ": " + option.getValue()).toList());
        }
    }

    public static List<MyOrderSummary> fromAll(List<OrderSummary> orders) {
        return orders.stream().map(MyOrderSummary::from).toList();
    }
}
