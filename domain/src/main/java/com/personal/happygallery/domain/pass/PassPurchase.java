package com.personal.happygallery.domain.pass;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.PassCreditInsufficientException;
import com.personal.happygallery.domain.error.PassExpiredException;
import com.personal.happygallery.domain.money.Proration;
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
import java.util.Objects;
import java.util.OptionalInt;

/** 이용권 구매 — pass_purchases 테이블 */
@Entity
@Table(name = "pass_purchases")
public class PassPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, updatable = false, length = 30)
    private PassPlan plan;

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

    private PassPurchase(Long userId,
                         LocalDateTime purchasedAt,
                         LocalDateTime expiresAt,
                         long totalPrice,
                         PassPlan plan) {
        this.userId = userId;
        this.purchasedAt = purchasedAt;
        this.expiresAt = expiresAt;
        this.plan = plan;
        this.totalCredits = plan.getTotalCredits();
        this.remainingCredits = totalCredits;
        this.totalPrice = totalPrice;
    }

    /** 회원 이용권 구매 생성. */
    public static PassPurchase forMember(Long userId,
                                         LocalDateTime purchasedAt,
                                         LocalDateTime expiresAt,
                                         long totalPrice,
                                         PassPlan plan) {
        if (userId == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이용권 회원 정보가 누락되었습니다.");
        }
        return new PassPurchase(userId, purchasedAt, expiresAt, totalPrice, plan);
    }

    /** 이용권이 해당 회원의 소유인지 확인한다. 회원 이용권만 존재하므로 null 회원은 항상 false다. */
    public boolean isOwnedBy(Long expectedUserId) {
        return Objects.equals(userId, expectedUserId);
    }

    /** 구매 시 확정한 상품 정책에 현재 클래스 카테고리가 포함되는지 검증한다. */
    public void requireApplicableToClass(String category, boolean passEligible) {
        if (!plan.supportsClass(category, passEligible)) {
            throw new HappyGalleryException(ErrorCode.PASS_NOT_APPLICABLE);
        }
    }

    /**
     * 만료/잔여 크레딧을 사용 직전에 검증한다.
     * expiresAt은 마지막 사용 가능일 다음날 00시를 나타내는 exclusive 만료 경계다.
     *
     * @param usedAt 이용권 사용 시각
     * @throws PassExpiredException          만료된 이용권
     * @throws PassCreditInsufficientException 잔여 크레딧 없음
     */
    public void requireUsableAt(LocalDateTime usedAt) {
        if (isExpiredAt(usedAt)) {
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

    /** expiresAt은 exclusive 경계이므로 해당 시각부터 만료로 본다. */
    public boolean isExpiredAt(LocalDateTime checkedAt) {
        return !checkedAt.isBefore(expiresAt);
    }

    /**
     * 예약 시 1크레딧을 소모한다. 만료와 잔여 크레딧을 변경 직전에 검증한다.
     *
     * @throws PassExpiredException 만료된 이용권
     * @throws PassCreditInsufficientException 잔여 크레딧이 0일 때
     */
    public void useCredit(LocalDateTime usedAt) {
        requireUsableAt(usedAt);
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

    /**
     * 만료 경계에 도달했다면 잔여 크레딧을 소멸시키고 소멸 수량을 반환한다.
     * 아직 유효하면 빈 결과를, 만료됐다면 이미 소멸한 경우까지 포함해 소멸 수량을 반환한다.
     */
    public OptionalInt expireIfReached(LocalDateTime checkedAt) {
        if (!isExpiredAt(checkedAt)) {
            return OptionalInt.empty();
        }
        int expiredCredits = remainingCredits;
        if (expiredCredits > 0) {
            expire();
        }
        return OptionalInt.of(expiredCredits);
    }

    /** 지정 크레딧을 총 결제액에 비례해 정산한다. 전 횟수 환불은 원결제액과 같다. */
    public long calculateRefundAmount(int refundCredits) {
        return refundAmount(totalPrice, refundCredits, totalCredits);
    }

    /** 남은 크레딧에 취소할 미래 예약 크레딧을 더하되 0~총 크레딧 범위를 넘지 않게 한다. */
    public static int refundableCredits(int remainingCredits, int cancelledFutureBookings, int totalCredits) {
        return Math.clamp(remainingCredits + cancelledFutureBookings, 0, totalCredits);
    }

    /** 환불 크레딧을 총 결제액에 비례해 원 단위 버림으로 정산한다. 관리자 예상 환불액도 이 식을 쓴다. */
    public static long refundAmount(long totalPrice, int refundCredits, int totalCredits) {
        return Proration.floor(totalPrice, refundCredits, totalCredits);
    }

    /** 결제 confirm 성공 후 원결제 paymentKey를 저장한다. */
    public void recordPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public PassPlan getPlan() { return plan; }
    public int getTotalCredits() { return totalCredits; }
    public int getRemainingCredits() { return remainingCredits; }
    public long getTotalPrice() { return totalPrice; }
    public String getPaymentKey() { return paymentKey; }
    public long getVersion() { return version; }
}
