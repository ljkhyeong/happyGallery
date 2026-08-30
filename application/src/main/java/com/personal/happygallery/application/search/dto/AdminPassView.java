package com.personal.happygallery.application.search.dto;

import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;

public record AdminPassView(
        Long passId,
        String passNumber,
        String customerName,
        String customerPhone,
        AdminPassStatus status,
        int remainingCredits,
        int totalCredits,
        LocalDateTime expiresAt,
        int futureBookingCount,
        long expectedRefundAmount,
        RefundStatus refundStatus
) {
}
