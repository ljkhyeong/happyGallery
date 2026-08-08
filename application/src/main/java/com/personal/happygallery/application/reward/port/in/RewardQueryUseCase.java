package com.personal.happygallery.application.reward.port.in;

import com.personal.happygallery.domain.reward.RewardLedgerType;
import java.time.LocalDateTime;
import java.util.List;

public interface RewardQueryUseCase {

    RewardWallet getWallet(Long userId);

    record RewardWallet(
            long availableBalance,
            long reservedBalance,
            long debtBalance,
            List<RewardHistory> history
    ) {}

    record RewardHistory(
            Long id,
            RewardLedgerType type,
            long amount,
            long availableAfter,
            long reservedAfter,
            long debtAfter,
            Long orderId,
            LocalDateTime createdAt
    ) {}
}
