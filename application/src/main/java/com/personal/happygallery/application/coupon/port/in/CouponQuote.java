package com.personal.happygallery.application.coupon.port.in;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/** 결제 준비 금액 계산용 쿠폰 견적. */
public record CouponQuote(
        Long issuedCouponId,
        long productAmount,
        long discountAmount
) {

    public CouponQuote {
        PaymentAmountPolicy.requireValid(productAmount);
        PaymentAmountPolicy.requireValid(discountAmount);
        if (discountAmount > productAmount) {
            throw new IllegalArgumentException("쿠폰 할인액은 상품 금액을 넘을 수 없습니다.");
        }
        if (issuedCouponId == null && discountAmount != 0L) {
            throw new IllegalArgumentException("미적용 쿠폰 견적의 할인액은 0원이어야 합니다.");
        }
    }

    public static CouponQuote none(long productAmount) {
        return new CouponQuote(null, productAmount, 0L);
    }

    public long discountedProductAmount() {
        return productAmount - discountAmount;
    }
}
