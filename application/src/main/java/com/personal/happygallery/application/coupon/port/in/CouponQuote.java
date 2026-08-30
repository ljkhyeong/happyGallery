package com.personal.happygallery.application.coupon.port.in;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/** 결제 준비 시 확정해 암호화 payload에 보관할 불변 쿠폰 견적. */
public record CouponQuote(
        Long issuedCouponId,
        Long definitionId,
        String name,
        long productAmount,
        long discountAmount
) {

    public CouponQuote {
        PaymentAmountPolicy.requireValid(productAmount);
        PaymentAmountPolicy.requireValid(discountAmount);
        if (discountAmount > productAmount) {
            throw new IllegalArgumentException("쿠폰 할인액은 상품 금액을 넘을 수 없습니다.");
        }
        boolean applied = issuedCouponId != null;
        if (applied != (definitionId != null) || applied != (name != null)) {
            throw new IllegalArgumentException("쿠폰 견적 식별 정보가 일치하지 않습니다.");
        }
        if (!applied && discountAmount != 0L) {
            throw new IllegalArgumentException("미적용 쿠폰 견적의 할인액은 0원이어야 합니다.");
        }
    }

    public static CouponQuote none(long productAmount) {
        return new CouponQuote(null, null, null, productAmount, 0L);
    }

    public boolean applied() {
        return issuedCouponId != null;
    }

    public long discountedProductAmount() {
        return productAmount - discountAmount;
    }
}
