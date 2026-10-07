package com.personal.happygallery.application.search.port.out;

import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;

public record AdminOrderSearchResult(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalAmount,
        String buyerNameEnc,
        String buyerPhoneEnc,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        LocalDateTime createdAt
) {
}
