package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Locale;

public enum SocialProvider {
    GOOGLE,
    NAVER,
    KAKAO;

    public static SocialProvider fromPath(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }
}
