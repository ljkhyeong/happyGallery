package com.personal.happygallery.domain.pass;

import com.personal.happygallery.common.error.PassCreditInsufficientException;
import com.personal.happygallery.common.error.PassExpiredException;
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
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/** 8회권 구매 — pass_purchases 테이블 */
@Entity
@Table(name = "pass_purchases")
public class PassPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

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

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    @Version
    @Column(nullable = false)
    private long version;

    protected PassPurchase() {}

    private PassPurchase(Guest guest, Long userId, LocalDateTime expiresAt, long totalPrice) {
        this.guest = guest;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.totalCredits = 8;
        this.remainingCredits = 8;
        this.totalPrice = totalPrice;
    }

    /** 회원 8회권 구매 생성. */
    public static PassPurchase forMember(Long userId, LocalDateTime expiresAt, long totalPrice) {
        return new PassPurchase(null, userId, expiresAt, totalPrice);
    }

    /** 기존 비회원 8회권 데이터 및 claim/테스트 생성을 위한 guest 소유 pass 생성. */
    public static PassPurchase forGuest(Guest guest, LocalDateTime expiresAt, long totalPrice) {
        return new PassPurchase(guest, null, expiresAt, totalPrice);
    }

    /**
     * 만료/잔여 크레딧 검증. 사용 전 호출한다.
     *
     * @param now 현재 시각
     * @throws PassExpiredException          만료된 이용권
     * @throws PassCreditInsufficientException 잔여 크레딧 없음
     */
    public void requireUsable(LocalDateTime now) {
        if (expiresAt.isBefore(now)) {
            throw new PassExpiredException();
        }
        if (!hasRemainingCredits()) {
            throw new PassCreditInsufficientException();
        }
    }

    /** 잔여 크레딧이 남아 있는지 확인한다. */
    public boolean hasRemainingCredits() {
        return remainingCredits > 0;
    }

    /**
     * 예약 시 1크레딧 소모.
     * 호출 전 USE ledger를 먼저 기록해야 한다.
     *
     * @throws PassCreditInsufficientException 잔여 크레딧이 0일 때
     */
    public void useCredit() {
        if (!hasRemainingCredits()) {
            throw new PassCreditInsufficientException();
        }
        this.remainingCredits--;
    }

    /**
     * 예약 취소(D-1 이전) 시 1크레딧 복구.
     * 호출 전 REFUND ledger를 먼저 기록해야 한다.
     */
    public void refundCredit() {
        this.remainingCredits++;
    }

    /**
     * 만료 처리 — 잔여 크레딧을 0으로 소멸시킨다.
     * 호출 전 EXPIRE ledger를 먼저 기록해야 한다.
     */
    public void expire() {
        this.remainingCredits = 0;
    }

    /** 크레딧 단가 = total_price / total_credits */
    public long unitPrice() {
        return totalCredits == 0 ? 0 : totalPrice / totalCredits;
    }

    /** 잔여 크레딧 기반 환불 금액 계산 = remaining_credits * unit_price */
    public long calculateRefundAmount() {
        return (long) remainingCredits * unitPrice();
    }

    public void claimToUser(Long userId) {
        this.userId = userId;
        this.guest = null;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Guest getGuest() { return guest; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getTotalCredits() { return totalCredits; }
    public int getRemainingCredits() { return remainingCredits; }
    public long getTotalPrice() { return totalPrice; }
    public long getVersion() { return version; }
}
