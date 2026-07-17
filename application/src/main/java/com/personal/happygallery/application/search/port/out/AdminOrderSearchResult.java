package com.personal.happygallery.application.search.port.out;

import java.time.LocalDateTime;

public record AdminOrderSearchResult(
        Long orderId,
        String orderNumber,
        String status,
        long totalAmount,
        String buyerNameEnc,
        String buyerPhoneEnc,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        LocalDateTime createdAt
) {
}
