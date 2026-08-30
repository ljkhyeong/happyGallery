package com.personal.happygallery.domain.category;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Locale;

/**
 * 사용자 입력 카테고리 표기 정규화.
 *
 * <p>카테고리는 운영자가 추가할 수 있는 표시/필터 값이므로 enum으로 고정하지 않고,
 * 저장·조회 기준 표기만 대문자 토큰으로 맞춘다.
 */
public final class CategoryName {

    private CategoryName() {}

    public static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public static String required(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "카테고리는 필수입니다.");
        }
        return normalized;
    }
}
