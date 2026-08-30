package com.personal.happygallery.domain.reward;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_lots")
public class RewardLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_order_id")
    private Long sourceOrderId;

    @Column(name = "earned_amount", nullable = false)
    private long earnedAmount;

    @Column(name = "remaining_amount", nullable = false)
    private long remainingAmount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RewardLot() {}

    public RewardLot(Long userId, Long sourceOrderId, long amount, LocalDateTime expiresAt) {
        if (userId == null || amount <= 0L || expiresAt == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 적립 단위가 올바르지 않습니다.");
        }
        this.userId = userId;
        this.sourceOrderId = sourceOrderId;
        this.earnedAmount = amount;
        this.remainingAmount = amount;
        this.expiresAt = expiresAt;
    }

    public long reserveUpTo(long requestedAmount, LocalDateTime now) {
        if (requestedAmount <= 0L || !expiresAt.isAfter(now) || remainingAmount == 0L) {
            return 0L;
        }
        long reserved = Math.min(remainingAmount, requestedAmount);
        remainingAmount -= reserved;
        return reserved;
    }

    public void release(long amount) {
        if (amount <= 0L || remainingAmount + amount > earnedAmount) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 적립 단위 반환액이 올바르지 않습니다.");
        }
        remainingAmount += amount;
    }

    public long expire(LocalDateTime now) {
        if (expiresAt.isAfter(now)) {
            return 0L;
        }
        long expired = remainingAmount;
        remainingAmount = 0L;
        return expired;
    }

    public long revokeUpTo(long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        long revoked = Math.min(remainingAmount, amount);
        remainingAmount -= revoked;
        return revoked;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSourceOrderId() { return sourceOrderId; }
    public long getEarnedAmount() { return earnedAmount; }
    public long getRemainingAmount() { return remainingAmount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
