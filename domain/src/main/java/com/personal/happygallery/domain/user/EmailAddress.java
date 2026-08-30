package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Locale;

public final class EmailAddress {

    public static final int MAX_LENGTH = 254;

    private EmailAddress() {}

    public static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이메일은 필수입니다.");
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "이메일은 254자 이하여야 합니다.");
        }
        return normalized;
    }

    public static String optional(String value) {
        return value == null ? null : required(value);
    }
}
