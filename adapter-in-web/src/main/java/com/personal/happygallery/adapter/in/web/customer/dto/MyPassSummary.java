package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase.PassView;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;

public record MyPassSummary(Long passId, String planCode, String planName,
                             LocalDateTime purchasedAt,
                             LocalDateTime expiresAt, int totalCredits,
                             int remainingCredits, long totalPrice,
                             RefundProgressResponse refund) {
    public static MyPassSummary from(PassView view) {
        PassPurchase p = view.pass();
        return new MyPassSummary(
                p.getId(), p.getPlan().name(), p.getPlan().getDisplayName(),
                p.getPurchasedAt(), p.getExpiresAt(),
                p.getTotalCredits(), p.getRemainingCredits(), p.getTotalPrice(),
                view.refund() == null ? null : RefundProgressResponse.from(view.refund()));
    }

    public static List<MyPassSummary> fromAll(List<PassView> passes) {
        return passes.stream().map(MyPassSummary::from).toList();
    }
}
