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
@Table(name = "reward_reservation_allocations")
public class RewardReservationAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "reward_lot_id", nullable = false)
    private Long rewardLotId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "restored_amount", nullable = false)
    private long restoredAmount;

    @Column(name = "original_expiry", nullable = false)
    private LocalDateTime originalExpiry;

    protected RewardReservationAllocation() {}

    public RewardReservationAllocation(
            Long reservationId, Long rewardLotId, long amount, LocalDateTime originalExpiry) {
        if (reservationId == null || rewardLotId == null || amount <= 0L || originalExpiry == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 예약 배분 정보가 올바르지 않습니다.");
        }
        this.reservationId = reservationId;
        this.rewardLotId = rewardLotId;
        this.amount = amount;
        this.originalExpiry = originalExpiry;
    }

    public long restoreUpTo(long requestedAmount) {
        if (requestedAmount <= 0L) {
            return 0L;
        }
        long restored = Math.min(amount - restoredAmount, requestedAmount);
        restoredAmount += restored;
        return restored;
    }

    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public Long getRewardLotId() { return rewardLotId; }
    public long getAmount() { return amount; }
    public long getRestoredAmount() { return restoredAmount; }
    public LocalDateTime getOriginalExpiry() { return originalExpiry; }
}
