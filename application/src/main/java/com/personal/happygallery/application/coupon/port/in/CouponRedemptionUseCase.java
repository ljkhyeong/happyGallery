package com.personal.happygallery.application.coupon.port.in;

import java.time.LocalDateTime;

/** ORDER 결제 준비·확정·해제·전액 취소와 쿠폰 상태를 연결하는 유스케이스. */
public interface CouponRedemptionUseCase {

    CouponQuote quoteAndLock(
            Long userId,
            Long issuedCouponId,
            long productAmount,
            LocalDateTime now);

    void reserve(Long issuedCouponId, Long paymentAttemptId);

    void redeem(Long issuedCouponId, Long paymentAttemptId, Long orderId);

    void release(Long issuedCouponId, Long paymentAttemptId);

    void restoreAfterFullCancellation(Long issuedCouponId, Long orderId, LocalDateTime now);
}
