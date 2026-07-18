package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.regex.Pattern;

public final class KoreanPhoneNumber {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^01[0-9]{8,9}$");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[\\s-]");

    private KoreanPhoneNumber() {}

    public static String required(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "휴대폰 번호는 필수입니다.");
        }
        return normalized;
    }

    public static String optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = SEPARATOR_PATTERN.matcher(value).replaceAll("");
        if (!MOBILE_PATTERN.matcher(normalized).matches()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "휴대폰 번호 형식이 올바르지 않습니다.");
        }
        return normalized;
    }
}
