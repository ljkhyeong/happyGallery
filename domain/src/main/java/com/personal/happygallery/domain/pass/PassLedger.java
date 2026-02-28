package com.personal.happygallery.domain.pass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 크레딧 원장 — pass_ledger 테이블 (append-only) */
@Entity
@Table(name = "pass_ledger")
public class PassLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_purchase_id", nullable = false)
    private PassPurchase passPurchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PassLedgerType type;

    @Column(nullable = false)
    private int amount;

    @Column(name = "related_booking_id")
    private Long relatedBookingId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PassLedger() {}

    /** EARN / EXPIRE — booking 없는 원장 기록 */
    public PassLedger(PassPurchase passPurchase, PassLedgerType type, int amount) {
        this.passPurchase = passPurchase;
        this.type = type;
        this.amount = amount;
    }

    /** USE / REFUND — booking 연결 원장 기록 */
    public PassLedger(PassPurchase passPurchase, PassLedgerType type, int amount, Long relatedBookingId) {
        this(passPurchase, type, amount);
        this.relatedBookingId = relatedBookingId;
    }

    public Long getId() { return id; }
    public PassPurchase getPassPurchase() { return passPurchase; }
    public PassLedgerType getType() { return type; }
    public int getAmount() { return amount; }
    public Long getRelatedBookingId() { return relatedBookingId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
