package com.personal.happygallery.domain.content;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

/** 문의·상품 Q&A·공지의 필수 입력과 글자 수 제한. */
public final class ContentTextPolicy {

    public static final int MIN_LENGTH = 1;
    public static final int MAX_TITLE_LENGTH = 200;

    /** MySQL utf8mb4 TEXT의 65,535 byte 한도를 모든 문자 조합에서 넘지 않는 상한. */
    public static final int MAX_BODY_LENGTH = 16_000;

    private ContentTextPolicy() {
    }

    public static String requireTitle(String value, String fieldName) {
        return requireText(value, fieldName, MAX_TITLE_LENGTH);
    }

    public static String requireBody(String value, String fieldName) {
        return requireText(value, fieldName, MAX_BODY_LENGTH);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "은 필수입니다.");
        }
        if (value.length() > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은 " + maxLength + "자 이하여야 합니다.");
        }
        return value;
    }
}
