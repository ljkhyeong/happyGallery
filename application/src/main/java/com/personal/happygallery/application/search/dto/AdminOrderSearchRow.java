package com.personal.happygallery.application.search.dto;

import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record AdminOrderSearchRow(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalAmount,
        String buyerName,
        String buyerPhone,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        OffsetDateTime createdAt
) {
}
