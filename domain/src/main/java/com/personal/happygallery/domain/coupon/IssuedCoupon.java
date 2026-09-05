package com.personal.happygallery.domain.coupon;

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
import java.util.Objects;

/** 회원에게 한 번 발급된 주문 쿠폰 — issued_coupons 테이블. */
@Entity
@Table(name = "issued_coupons")
public class IssuedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false, updatable = false)
    private Long definitionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedCouponStatus status;

    @Column(name = "payment_attempt_id")
    private Long paymentAttemptId;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private LocalDateTime claimedAt;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected IssuedCoupon() {}

    public IssuedCoupon(Long definitionId, Long userId, LocalDateTime claimedAt) {
        if (definitionId == null || userId == null || claimedAt == null) {
            throw invalid("쿠폰 발급 정보가 누락되었습니다.");
        }
        this.definitionId = definitionId;
        this.userId = userId;
        this.claimedAt = claimedAt;
        this.status = IssuedCouponStatus.AVAILABLE;
    }

    /** 견적 계산 전에 만료를 반영하고 사용 가능한 상태인지 검증한다. */
    public void requireAvailableAt(LocalDateTime validUntil, LocalDateTime now) {
        expireIfReached(validUntil, now);
        if (status != IssuedCouponStatus.AVAILABLE) {
            throw conflict("사용 가능한 상태의 쿠폰이 아닙니다.");
        }
    }

    /** AVAILABLE 쿠폰만 결제 시도에 예약한다. 같은 결제 시도의 재호출은 멱등 처리한다. */
    public void reserve(Long attemptId, LocalDateTime now) {
        requireId(attemptId, "결제 시도");
        requireTime(now);
        if (status == IssuedCouponStatus.RESERVED
                && Objects.equals(paymentAttemptId, attemptId)) {
            return;
        }
        if (status != IssuedCouponStatus.AVAILABLE) {
            throw conflict("예약할 수 없는 상태의 쿠폰입니다.");
        }
        status = IssuedCouponStatus.RESERVED;
        paymentAttemptId = attemptId;
        reservedAt = now;
    }

    /** 예약을 소유한 결제 시도만 주문에 쿠폰을 사용 완료할 수 있다. */
    public void redeem(Long attemptId, Long orderId, LocalDateTime now) {
        requireId(attemptId, "결제 시도");
        requireId(orderId, "주문");
        requireTime(now);
        if (status == IssuedCouponStatus.REDEEMED
                && Objects.equals(paymentAttemptId, attemptId)
                && Objects.equals(usedOrderId, orderId)) {
            return;
        }
        requireReservedBy(attemptId);
        status = IssuedCouponStatus.REDEEMED;
        usedOrderId = orderId;
        usedAt = now;
    }

    /** 결제 실패·만료 시 예약을 해제한다. 해제 시점이 만료 뒤면 EXPIRED로 전이한다. */
    public void release(Long attemptId, LocalDateTime validUntil, LocalDateTime now) {
        requireId(attemptId, "결제 시도");
        requireTime(validUntil);
        requireTime(now);
        if ((status == IssuedCouponStatus.AVAILABLE || status == IssuedCouponStatus.EXPIRED)
                && paymentAttemptId == null) {
            return;
        }
        requireReservedBy(attemptId);
        paymentAttemptId = null;
        reservedAt = null;
        status = !now.isBefore(validUntil)
                ? IssuedCouponStatus.EXPIRED
                : IssuedCouponStatus.AVAILABLE;
    }

    /** 전액 취소된 주문의 쿠폰은 유효기간 안에서만 다시 AVAILABLE로 복원한다. */
    public void restoreAfterFullCancellation(Long orderId,
                                             LocalDateTime validUntil,
                                             LocalDateTime now) {
        requireId(orderId, "주문");
        requireTime(validUntil);
        requireTime(now);
        if (status == IssuedCouponStatus.AVAILABLE && usedOrderId == null) {
            return;
        }
        if (status == IssuedCouponStatus.EXPIRED
                && Objects.equals(usedOrderId, orderId)) {
            return;
        }
        if (status != IssuedCouponStatus.REDEEMED
                || !Objects.equals(usedOrderId, orderId)) {
            throw conflict("해당 주문에 사용된 쿠폰이 아닙니다.");
        }
        if (!now.isBefore(validUntil)) {
            status = IssuedCouponStatus.EXPIRED;
            return;
        }
        status = IssuedCouponStatus.AVAILABLE;
        paymentAttemptId = null;
        usedOrderId = null;
        reservedAt = null;
        usedAt = null;
    }

    /** 미사용 쿠폰을 취소한다. 결제 예약·사용 완료 상태는 취소할 수 없다. */
    public void cancel() {
        if (status == IssuedCouponStatus.CANCELED) {
            return;
        }
        if (status == IssuedCouponStatus.RESERVED || status == IssuedCouponStatus.REDEEMED) {
            throw conflict("결제 처리 중이거나 사용된 쿠폰은 취소할 수 없습니다.");
        }
        status = IssuedCouponStatus.CANCELED;
        paymentAttemptId = null;
        usedOrderId = null;
        reservedAt = null;
        usedAt = null;
    }

    /** 사용 가능한 쿠폰은 validUntil부터 만료 처리한다. */
    public boolean expireIfReached(LocalDateTime validUntil, LocalDateTime now) {
        requireTime(validUntil);
        requireTime(now);
        if (status != IssuedCouponStatus.AVAILABLE || now.isBefore(validUntil)) {
            return false;
        }
        status = IssuedCouponStatus.EXPIRED;
        return true;
    }

    public boolean isOwnedBy(Long expectedUserId) {
        return Objects.equals(userId, expectedUserId);
    }

    private void requireReservedBy(Long attemptId) {
        if (status != IssuedCouponStatus.RESERVED
                || !Objects.equals(paymentAttemptId, attemptId)) {
            throw conflict("이 결제 시도에 예약된 쿠폰이 아닙니다.");
        }
    }

    private static void requireId(Long id, String fieldName) {
        if (id == null || id < 1L) {
            throw invalid(fieldName + " 식별자가 올바르지 않습니다.");
        }
    }

    private static void requireTime(LocalDateTime time) {
        if (time == null) {
            throw invalid("쿠폰 기준 시각이 누락되었습니다.");
        }
    }

    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }

    private static HappyGalleryException conflict(String message) {
        return new HappyGalleryException(ErrorCode.CONFLICT, message);
    }

    public Long getId() { return id; }
    public Long getDefinitionId() { return definitionId; }
    public Long getUserId() { return userId; }
    public IssuedCouponStatus getStatus() { return status; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public Long getUsedOrderId() { return usedOrderId; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public LocalDateTime getReservedAt() { return reservedAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public long getVersion() { return version; }
}
