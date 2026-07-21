package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.OrderItem;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record AdminOrderResponse(
        Long orderId,
        String orderNumber,
        String status,
        long totalAmount,
        long shippingFee,
        String fulfillmentType,
        List<Item> items,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        OffsetDateTime createdAt
) {

    public AdminOrderResponse {
        items = List.copyOf(items);
    }

    public record Item(Long productId, String productName, int qty, long unitPrice) {
        private static Item from(OrderItem item) {
            return new Item(
                    item.getProductId(), item.getProductName(), item.getQty(), item.getUnitPrice());
        }
    }

    public static AdminOrderResponse from(
            Order order, Fulfillment fulfillment, List<OrderItem> orderItems) {
        return new AdminOrderResponse(
                order.getId(),
                "ORD-%08d".formatted(order.getId()),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getShippingFee(),
                fulfillment == null ? null : fulfillment.getType().name(),
                orderItems.stream().map(Item::from).toList(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                order.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
