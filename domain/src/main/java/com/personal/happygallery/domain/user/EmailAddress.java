package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Locale;

public final class EmailAddress {

    private EmailAddress() {}

    public static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이메일은 필수입니다.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
