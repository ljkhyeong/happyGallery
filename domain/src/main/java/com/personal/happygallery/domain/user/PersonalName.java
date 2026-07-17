package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public final class PersonalName {

    private static final int MAX_LENGTH = 100;

    private PersonalName() {}

    public static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이름은 필수입니다.");
        }
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) > MAX_LENGTH) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이름은 100자 이하여야 합니다.");
        }
        return normalized;
    }
}
