package com.personal.happygallery.domain.money;

import java.math.BigInteger;

/** 금액을 비율로 나눌 때 쓰는 원 단위 버림 계산. */
public final class Proration {

    private Proration() {}

    /**
     * {@code amount × numerator / denominator}의 몫을 원 단위로 버림해 돌려준다.
     * 중간 곱은 BigInteger로 계산하므로 long 범위를 넘어도 안전하다.
     * 0으로 나누거나 몫이 long 범위를 벗어나면 {@link ArithmeticException}을 그대로 던진다.
     */
    public static long floor(long amount, long numerator, long denominator) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(numerator))
                .divide(BigInteger.valueOf(denominator))
                .longValueExact();
    }
}
