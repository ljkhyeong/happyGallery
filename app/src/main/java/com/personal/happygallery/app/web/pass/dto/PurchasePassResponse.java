package com.personal.happygallery.app.web.pass.dto;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;

public record PurchasePassResponse(
        Long passId,
        Long guestId,
        LocalDateTime expiresAt,
        int totalCredits,
        int remainingCredits,
        long totalPrice
) {
    public static PurchasePassResponse from(PassPurchase p) {
        return new PurchasePassResponse(
                p.getId(),
                p.getGuest().getId(),
                p.getExpiresAt(),
                p.getTotalCredits(),
                p.getRemainingCredits(),
                p.getTotalPrice()
        );
    }
}
