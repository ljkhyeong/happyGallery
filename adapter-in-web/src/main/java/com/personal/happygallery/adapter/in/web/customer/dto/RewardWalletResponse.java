package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase.RewardWallet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RewardWalletResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long availableBalance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long reservedBalance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long debtBalance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RewardHistoryResponse> history
) {
    public static RewardWalletResponse from(RewardWallet wallet) {
        return new RewardWalletResponse(
                wallet.availableBalance(), wallet.reservedBalance(), wallet.debtBalance(),
                wallet.history().stream().map(RewardHistoryResponse::from).toList());
    }
}
