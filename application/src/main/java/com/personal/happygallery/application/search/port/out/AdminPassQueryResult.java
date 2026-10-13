package com.personal.happygallery.application.search.port.out;

import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;

public record AdminPassQueryResult(
        Long passId,
        String passNumber,
        String customerNameEnc,
        String customerPhoneEnc,
        LocalDateTime expiresAt,
        int totalCredits,
        int remainingCredits,
        long totalPrice,
        int futureBookingCount,
        Long refundAmount,
        RefundStatus refundStatus
) {
}
