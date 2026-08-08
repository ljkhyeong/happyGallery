package com.personal.happygallery.domain.media;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.net.URI;

/** 상품과 클래스가 저장하는 대표 이미지 참조의 형식과 길이를 정의한다. */
public final class ImageReferencePolicy {

    public static final int MAX_LENGTH = 500;

    private ImageReferencePolicy() {}

    /** 비어 있거나, 서비스 절대 경로이거나, 호스트가 있는 HTTP(S) URL인 이미지 참조를 반환한다. */
    public static String optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_LENGTH) {
            throw invalidReference("대표 이미지 URL은 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        try {
            URI reference = URI.create(normalized);
            if (isServicePath(normalized, reference) || isHttpUrl(reference)) {
                return normalized;
            }
        } catch (IllegalArgumentException ignored) {
            // 아래의 일관된 도메인 오류로 변환한다.
        }
        throw invalidReference("대표 이미지 URL은 http(s) 주소 또는 /로 시작하는 경로여야 합니다.");
    }

    private static boolean isServicePath(String value, URI reference) {
        String path = reference.getRawPath();
        return !value.startsWith("//")
                && reference.getScheme() == null
                && reference.getRawAuthority() == null
                && path != null
                && path.startsWith("/");
    }

    private static boolean isHttpUrl(URI reference) {
        String scheme = reference.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && reference.getHost() != null;
    }

    private static HappyGalleryException invalidReference(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }
}
