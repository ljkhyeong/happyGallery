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
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_ledger")
public class RewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardLedgerType type;

    @Column(nullable = false)
    private long amount;

    @Column(name = "available_after", nullable = false)
    private long availableAfter;

    @Column(name = "reserved_after", nullable = false)
    private long reservedAfter;

    @Column(name = "debt_after", nullable = false)
    private long debtAfter;

    @Column(name = "payment_attempt_id")
    private Long paymentAttemptId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RewardLedger() {}

    public RewardLedger(Long userId,
                        RewardLedgerType type,
                        long amount,
                        RewardAccount account,
                        Long paymentAttemptId,
                        Long orderId,
                        String idempotencyKey) {
        if (userId == null || type == null || amount <= 0L || account == null
                || idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 160) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 원장 정보가 올바르지 않습니다.");
        }
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.availableAfter = account.getAvailableBalance();
        this.reservedAfter = account.getReservedBalance();
        this.debtAfter = account.getDebtBalance();
        this.paymentAttemptId = paymentAttemptId;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public RewardLedgerType getType() { return type; }
    public long getAmount() { return amount; }
    public long getAvailableAfter() { return availableAfter; }
    public long getReservedAfter() { return reservedAfter; }
    public long getDebtAfter() { return debtAfter; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public Long getOrderId() { return orderId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
