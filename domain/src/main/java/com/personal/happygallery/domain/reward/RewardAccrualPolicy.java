package com.personal.happygallery.domain.reward;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/** 회원 상품 주문의 실결제 상품 금액에 적용하는 적립 정책. */
public final class RewardAccrualPolicy {

    private static final long RATE_PERCENT = 1L;

    private RewardAccrualPolicy() {}

    /** 1원 미만은 버리는 정수 원 단위 적립액을 계산한다. */
    public static long calculate(long rewardEarnBase) {
        PaymentAmountPolicy.requireValid(rewardEarnBase);
        return rewardEarnBase * RATE_PERCENT / 100L;
    }
}
