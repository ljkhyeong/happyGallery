package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record AdminOrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalAmount,
        long shippingFee,
        FulfillmentType fulfillmentType,
        List<OrderItemView> items,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        OffsetDateTime createdAt
) {

    public AdminOrderResponse {
        items = List.copyOf(items);
    }

    public record OrderItemView(
            Long productId,
            String productName,
            ProductType productType,
            int qty,
            long unitPrice,
            String specification,
            String careInstructions,
            Integer productionLeadDays
    ) {
        private static OrderItemView from(OrderItem item) {
            return new OrderItemView(
                    item.getProductId(),
                    item.getProductName(),
                    item.getProductType(),
                    item.getQty(),
                    item.getUnitPrice(),
                    item.getSpecification(),
                    item.getCareInstructions(),
                    item.getProductionLeadDays());
        }
    }

    public static AdminOrderResponse from(
            Order order, Fulfillment fulfillment, List<OrderItem> orderItems) {
        return new AdminOrderResponse(
                order.getId(),
                "ORD-%08d".formatted(order.getId()),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingFee(),
                fulfillment == null ? null : fulfillment.getType(),
                orderItems.stream().map(OrderItemView::from).toList(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                order.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
