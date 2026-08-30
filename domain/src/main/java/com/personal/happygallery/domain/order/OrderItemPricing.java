package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/** 품목별 할인·적립금 배분 스냅샷. */
public record OrderItemPricing(
        long grossAmount,
        long couponDiscountAmount,
        long rewardUsedAmount,
        long netPaidAmount
) {

    public OrderItemPricing {
        PaymentAmountPolicy.requireValid(grossAmount);
        PaymentAmountPolicy.requireValid(couponDiscountAmount);
        PaymentAmountPolicy.requireValid(rewardUsedAmount);
        PaymentAmountPolicy.requireValid(netPaidAmount);
        if (grossAmount == 0L
                || couponDiscountAmount > grossAmount
                || rewardUsedAmount > grossAmount - couponDiscountAmount
                || netPaidAmount != grossAmount - couponDiscountAmount - rewardUsedAmount) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 품목 혜택 배분이 올바르지 않습니다.");
        }
    }

    public static OrderItemPricing fullPrice(int quantity, long unitPrice) {
        long gross = OrderAmountCalculator.addLine(0L, quantity, unitPrice);
        return new OrderItemPricing(gross, 0L, 0L, gross);
    }
}
