package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;

public record AdminOrderResponse(
        Long orderId,
        String orderNumber,
        String status,
        long totalAmount,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        LocalDateTime createdAt
) {

    public static AdminOrderResponse from(Order order) {
        return new AdminOrderResponse(
                order.getId(),
                "ORD-%08d".formatted(order.getId()),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                order.getCreatedAt()
        );
    }
}
