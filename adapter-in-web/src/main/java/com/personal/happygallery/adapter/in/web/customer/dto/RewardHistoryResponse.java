package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase.RewardHistory;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record RewardHistoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RewardLedgerType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long availableAfter,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long reservedAfter,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long debtAfter,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static RewardHistoryResponse from(RewardHistory history) {
        return new RewardHistoryResponse(
                history.id(), history.type(), history.amount(), history.availableAfter(),
                history.reservedAfter(), history.debtAfter(), history.orderId(), history.createdAt());
    }
}
