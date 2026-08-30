package com.personal.happygallery.domain.payment;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public final class PaymentAmountPolicy {

    /** 브라우저와 JSON number가 원 단위 정수를 정확히 표현할 수 있는 상한. */
    public static final long MAX_AMOUNT = 9_007_199_254_740_991L;

    private PaymentAmountPolicy() {}

    public static void requireValid(long amount) {
        if (amount < 0L || amount > MAX_AMOUNT) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "결제 금액이 허용 범위를 초과했습니다.");
        }
    }
}
