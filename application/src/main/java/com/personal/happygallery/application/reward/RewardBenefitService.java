package com.personal.happygallery.application.reward;

import java.time.LocalDateTime;

/** 결제·환불 흐름이 적립금 저장 구조를 직접 알지 않도록 하는 내부 금융 경계. */
public interface RewardBenefitService {

    long quoteAndLock(Long userId, long requestedAmount, long maxUsableAmount, LocalDateTime now);

    void reserve(Long userId, long amount, Long paymentAttemptId, LocalDateTime now);

    void release(Long paymentAttemptId, LocalDateTime now);

    void consume(Long paymentAttemptId, Long orderId, long expectedAmount, LocalDateTime now);

    void restoreUsed(Long orderId, long amount, String idempotencyKey, LocalDateTime now);

    void accrue(Long userId, Long orderId, long amount, LocalDateTime earnedAt);

    void revokeEarned(Long userId, Long orderId, long amount, String idempotencyKey);

    RewardEarnedSnapshot getEarnedSnapshot(Long orderId);

    record RewardEarnedSnapshot(long earnedAmount, long revokedAmount) {

        public RewardEarnedSnapshot {
            if (earnedAmount < 0L || revokedAmount < 0L || revokedAmount > earnedAmount) {
                throw new IllegalArgumentException("적립·회수 누계가 올바르지 않습니다.");
            }
        }

        public long remainingAmount() {
            return earnedAmount - revokedAmount;
        }

        public static RewardEarnedSnapshot none() {
            return new RewardEarnedSnapshot(0L, 0L);
        }
    }
}
