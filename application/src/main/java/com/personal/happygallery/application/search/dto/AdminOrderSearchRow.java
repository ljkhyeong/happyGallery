package com.personal.happygallery.application.search.dto;

import java.time.LocalDateTime;

public record AdminOrderSearchRow(
        Long orderId,
        String orderNumber,
        String status,
        long totalAmount,
        String buyerName,
        String buyerPhone,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        LocalDateTime createdAt
) {
}
