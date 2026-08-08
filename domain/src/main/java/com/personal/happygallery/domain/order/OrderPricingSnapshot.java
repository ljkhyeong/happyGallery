package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/** 결제 준비에서 서버가 확정해 주문에 영구 보존하는 혜택 적용 금액. */
public record OrderPricingSnapshot(
        long productAmount,
        long shippingFee,
        long couponDiscountAmount,
        long rewardUsedAmount,
        long pgPaidAmount,
        Long issuedCouponId
) {

    public OrderPricingSnapshot {
        PaymentAmountPolicy.requireValid(productAmount);
        PaymentAmountPolicy.requireValid(shippingFee);
        PaymentAmountPolicy.requireValid(couponDiscountAmount);
        PaymentAmountPolicy.requireValid(rewardUsedAmount);
        PaymentAmountPolicy.requireValid(pgPaidAmount);
        if (productAmount == 0L) {
            throw invalid();
        }
        if ((issuedCouponId == null) != (couponDiscountAmount == 0L)) {
            throw invalid();
        }
        if (couponDiscountAmount > productAmount
                || rewardUsedAmount > productAmount - couponDiscountAmount) {
            throw invalid();
        }
        if (pgPaidAmount != exactSubtract(totalAmountOf(
                productAmount, shippingFee, couponDiscountAmount), rewardUsedAmount)) {
            throw invalid();
        }
    }

    public static OrderPricingSnapshot fullPrice(long productAmount, long shippingFee) {
        return new OrderPricingSnapshot(
                productAmount, shippingFee, 0L, 0L,
                totalAmountOf(productAmount, shippingFee, 0L), null);
    }

    /** 쿠폰 적용 뒤 고객이 적립금과 PG로 지불한 주문 총액. */
    public long totalAmount() {
        return totalAmountOf(productAmount, shippingFee, couponDiscountAmount);
    }

    /** 배송비와 적립금 사용을 제외한 신규 적립 기준 상품 금액. */
    public long rewardEarnBase() {
        return productAmount - couponDiscountAmount - rewardUsedAmount;
    }

    private static long totalAmountOf(long productAmount, long shippingFee, long couponDiscount) {
        try {
            long gross = Math.addExact(productAmount, shippingFee);
            long total = Math.subtractExact(gross, couponDiscount);
            PaymentAmountPolicy.requireValid(total);
            return total;
        } catch (ArithmeticException exception) {
            throw invalid();
        }
    }

    private static long exactSubtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException exception) {
            throw invalid();
        }
    }

    private static HappyGalleryException invalid() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 혜택 금액 구성이 올바르지 않습니다.");
    }
}
