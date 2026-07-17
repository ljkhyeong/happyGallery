package com.personal.happygallery.application.search.port.out;

import java.time.LocalDateTime;

public record AdminOrderSearchResult(
        Long orderId,
        String orderNumber,
        String status,
        long totalAmount,
        String buyerName,
        String memberPhone,
        String guestPhoneEnc,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        LocalDateTime createdAt
) {
}
