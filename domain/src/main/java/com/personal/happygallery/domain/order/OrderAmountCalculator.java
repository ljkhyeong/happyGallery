package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/** 주문 수량과 금액의 공통 계산 규칙. */
public final class OrderAmountCalculator {

    public static final int MAX_ITEM_QUANTITY = 99;

    private OrderAmountCalculator() {}

    public static long addLine(long currentTotal, int quantity, long unitPrice) {
        requireQuantity(quantity);
        if (unitPrice <= 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "상품 단가는 1원 이상이어야 합니다.");
        }
        try {
            long total = Math.addExact(currentTotal, Math.multiplyExact(unitPrice, quantity));
            PaymentAmountPolicy.requireValid(total);
            return total;
        } catch (ArithmeticException e) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 금액이 허용 범위를 초과했습니다.");
        }
    }

    public static long addShippingFee(long itemTotal, long shippingFee) {
        if (shippingFee < 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "배송비는 0원 이상이어야 합니다.");
        }
        try {
            long total = Math.addExact(itemTotal, shippingFee);
            PaymentAmountPolicy.requireValid(total);
            return total;
        } catch (ArithmeticException e) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 금액이 허용 범위를 초과했습니다.");
        }
    }

    public static void requireQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_ITEM_QUANTITY) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "상품별 주문 수량은 1개 이상 " + MAX_ITEM_QUANTITY + "개 이하여야 합니다.");
        }
    }
}
