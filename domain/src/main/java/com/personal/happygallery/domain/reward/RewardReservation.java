package com.personal.happygallery.domain.reward;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_reservations")
public class RewardReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_attempt_id", nullable = false)
    private Long paymentAttemptId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "restored_amount", nullable = false)
    private long restoredAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardReservationStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected RewardReservation() {}

    public RewardReservation(Long paymentAttemptId, Long userId, long amount) {
        if (paymentAttemptId == null || userId == null || amount <= 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 예약 정보가 올바르지 않습니다.");
        }
        this.paymentAttemptId = paymentAttemptId;
        this.userId = userId;
        this.amount = amount;
        this.status = RewardReservationStatus.RESERVED;
    }

    public void markUsed(Long orderId, LocalDateTime now) {
        requireStatus(RewardReservationStatus.RESERVED);
        if (orderId == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 사용 주문은 필수입니다.");
        }
        this.orderId = orderId;
        this.status = RewardReservationStatus.USED;
        this.resolvedAt = now;
    }

    public void release(LocalDateTime now) {
        requireStatus(RewardReservationStatus.RESERVED);
        this.status = RewardReservationStatus.RELEASED;
        this.resolvedAt = now;
    }

    public void recordRestored(long amount) {
        requireStatus(RewardReservationStatus.USED);
        if (amount <= 0L || restoredAmount + amount > this.amount) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 복원 누적액이 사용액을 초과합니다.");
        }
        restoredAmount += amount;
    }

    public long restorableAmount() {
        return amount - restoredAmount;
    }

    private void requireStatus(RewardReservationStatus expected) {
        if (status != expected) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "현재 상태에서는 적립금을 변경할 수 없습니다.");
        }
    }

    public Long getId() { return id; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public Long getUserId() { return userId; }
    public Long getOrderId() { return orderId; }
    public long getAmount() { return amount; }
    public long getRestoredAmount() { return restoredAmount; }
    public RewardReservationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
