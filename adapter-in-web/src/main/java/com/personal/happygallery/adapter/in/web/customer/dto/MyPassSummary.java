package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;

public record MyPassSummary(Long passId, LocalDateTime purchasedAt,
                             LocalDateTime expiresAt, int totalCredits,
                             int remainingCredits, long totalPrice) {
    public static MyPassSummary from(PassPurchase p) {
        return new MyPassSummary(p.getId(), p.getPurchasedAt(), p.getExpiresAt(),
                p.getTotalCredits(), p.getRemainingCredits(), p.getTotalPrice());
    }

    public static List<MyPassSummary> fromAll(List<PassPurchase> passes) {
        return passes.stream().map(MyPassSummary::from).toList();
    }
}
