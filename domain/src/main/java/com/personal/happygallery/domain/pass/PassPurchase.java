package com.personal.happygallery.domain.pass;

import com.personal.happygallery.domain.error.PassCreditInsufficientException;
import com.personal.happygallery.domain.error.PassExpiredException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Version
    @Column(nullable = false)
    private long version;

    protected PassPurchase() {}

    private PassPurchase(Long userId, LocalDateTime expiresAt, long totalPrice) {
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.totalCredits = 8;
        this.remainingCredits = 8;
        this.totalPrice = totalPrice;
    }

    /** 회원 8회권 구매 생성. */
    public static PassPurchase forMember(Long userId, LocalDateTime expiresAt, long totalPrice) {
        return new PassPurchase(userId, expiresAt, totalPrice);
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

    /** 결제 confirm 성공 후 PG 원결제 참조값을 저장한다. */
    public void recordPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getTotalCredits() { return totalCredits; }
    public int getRemainingCredits() { return remainingCredits; }
    public long getTotalPrice() { return totalPrice; }
    public String getPaymentKey() { return paymentKey; }
    public long getVersion() { return version; }
}
