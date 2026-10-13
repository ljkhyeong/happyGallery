package com.personal.happygallery.domain.product;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.text.DomainText;
import java.util.regex.Pattern;

public final class ProductOptionPolicy {

    public static final int MAX_SELECT_GROUPS = 3;
    public static final int MAX_TEXT_GROUPS = 5;
    public static final int MAX_COMBINATIONS = 500;
    public static final int MAX_NAME_LENGTH = 25;
    public static final int MAX_KEY_LENGTH = 64;
    public static final int MAX_INPUT_LENGTH = 200;
    public static final int MAX_PLACEHOLDER_LENGTH = 100;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private ProductOptionPolicy() {}

    public static String requireKey(String value, String fieldName) {
        if (value == null || !KEY_PATTERN.matcher(value).matches()) {
            throw invalid(fieldName + " 식별자가 올바르지 않습니다.");
        }
        return value;
    }

    /** 옵션·옵션값 이름. {@code subject}는 "옵션명은"처럼 조사를 포함한다. */
    public static String requireName(String value, String subject) {
        return DomainText.required(value, subject, MAX_NAME_LENGTH);
    }

    public static long requireTextPriceAdjustment(long value) {
        if (value < 0 || value > PaymentAmountPolicy.MAX_AMOUNT) {
            throw invalid("직접입력형 옵션 추가 금액이 올바르지 않습니다.");
        }
        return value;
    }

    public static long requireVariantPrice(long basePrice, long adjustment) {
        try {
            long finalPrice = Math.addExact(basePrice, adjustment);
            if (finalPrice < 1 || finalPrice > PaymentAmountPolicy.MAX_AMOUNT) {
                throw invalid("옵션 조합의 최종 가격이 허용 범위를 벗어났습니다.");
            }
            return adjustment;
        } catch (ArithmeticException exception) {
            throw invalid("옵션 조합의 최종 가격이 허용 범위를 벗어났습니다.");
        }
    }

    public static int requireSortOrder(int value) {
        if (value < 0) {
            throw invalid("옵션 정렬 순서는 0 이상이어야 합니다.");
        }
        return value;
    }

    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }
}
