package com.personal.happygallery.domain.text;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

/**
 * 엔티티 문자열의 공통 정규화·길이 검증·자르기.
 *
 * <p>앞뒤 공백을 {@link String#strip()}으로 지우고 utf8mb4 VARCHAR와 같은 문자(code point) 수로 길이를 센다.
 * {@code subject}는 "클래스명은"처럼 조사를 포함한 주어로 받아 오류 문구를 만든다.
 */
public final class DomainText {

    private DomainText() {}

    public static String required(String value, String subject, int maxLength) {
        String normalized = optional(value, subject, maxLength);
        if (normalized == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, subject + " 필수입니다.");
        }
        return normalized;
    }

    /** 비어 있으면 null을 돌려준다. */
    public static String optional(String value, String subject, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, subject + " " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    /**
     * 외부 시스템 오류 문구처럼 거절할 수 없는 값을 저장 컬럼 길이에 맞게 앞에서 자른다. null은 그대로 돌려준다.
     * 검증과 달리 code point가 아닌 {@link String#length()}(UTF-16 단위) 기준이라 보충 문자 경계에서는 한 문자가 잘릴 수 있다.
     */
    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
