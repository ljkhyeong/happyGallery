package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase.PassView;
import com.personal.happygallery.domain.pass.PassPlan;
import com.personal.happygallery.domain.pass.PassPurchase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record MyPassSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long passId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PassPlan planCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String planName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime purchasedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int remainingCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        RefundProgressResponse refund) {
    public static MyPassSummary from(PassView view) {
        PassPurchase p = view.pass();
        return new MyPassSummary(
                p.getId(), p.getPlan(), p.getPlan().getDisplayName(),
                p.getPurchasedAt(), p.getExpiresAt(),
                p.getTotalCredits(), p.getRemainingCredits(), p.getTotalPrice(),
                view.refund() == null ? null : RefundProgressResponse.from(view.refund()));
    }

    public static List<MyPassSummary> fromAll(List<PassView> passes) {
        return passes.stream().map(MyPassSummary::from).toList();
    }
}
