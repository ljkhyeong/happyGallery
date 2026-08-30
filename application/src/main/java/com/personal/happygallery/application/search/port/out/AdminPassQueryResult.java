package com.personal.happygallery.application.search.port.out;

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
        String refundStatus
) {
}
