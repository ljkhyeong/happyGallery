package com.personal.happygallery.application.shared.page;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Base64;

/** 조회 조건과 정렬값을 함께 보관하는 회원 이력 커서. */
public record MemberHistoryCursor(String value, long id) {
    public static String encode(String scope, Object value, Long id) {
        return encodeText("member-v1|" + encodeText(scope) + "|" + value + "|" + id);
    }

    public static MemberHistoryCursor decode(String cursor, String scope) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                    .split("\\|", -1);
            if (parts.length != 4 || !parts[0].equals("member-v1")
                    || !parts[1].equals(encodeText(scope))) {
                throw new IllegalArgumentException();
            }
            long id = Long.parseLong(parts[3]);
            if (id < 1) throw new IllegalArgumentException();
            return new MemberHistoryCursor(parts[2], id);
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    public Object sortValue(boolean numeric) {
        try {
            if (!numeric) return LocalDateTime.parse(value);
            long number = Long.parseLong(value);
            if (number < 0) throw new IllegalArgumentException();
            return number;
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static HappyGalleryException invalidCursor() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT,
                "검색 조건이 바뀌었거나 페이지 커서가 올바르지 않습니다. 처음부터 조회해 주세요.");
    }
}
