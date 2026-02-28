package com.personal.happygallery.domain.pass;

import com.personal.happygallery.domain.booking.Guest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 8회권 구매 — pass_purchases 테이블 */
@Entity
@Table(name = "pass_purchases")
public class PassPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @Column(name = "purchased_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "total_credits", nullable = false)
    private int totalCredits;

    @Column(name = "remaining_credits", nullable = false)
    private int remainingCredits;

    protected PassPurchase() {}

    /**
     * 게스트 8회권 구매 생성.
     *
     * @param guest     구매자 (비회원)
     * @param expiresAt 만료 시점 = purchased_at + 90일
     */
    public PassPurchase(Guest guest, LocalDateTime expiresAt) {
        this.guest = guest;
        this.expiresAt = expiresAt;
        this.totalCredits = 8;
        this.remainingCredits = 8;
    }

    /**
     * 만료 처리 — 잔여 크레딧을 0으로 소멸시킨다.
     * 호출 전 EXPIRE ledger를 먼저 기록해야 한다.
     */
    public void expire() {
        this.remainingCredits = 0;
    }

    public Long getId() { return id; }
    public Guest getGuest() { return guest; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getTotalCredits() { return totalCredits; }
    public int getRemainingCredits() { return remainingCredits; }
}
