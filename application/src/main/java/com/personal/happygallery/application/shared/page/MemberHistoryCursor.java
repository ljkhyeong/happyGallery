package com.personal.happygallery.application.shared.page;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.DateTimeException;
import java.time.LocalDateTime;

/** 조회 조건과 정렬값을 함께 보관하는 회원 이력 커서. */
public record MemberHistoryCursor(String value, long id) {

    private static final String VERSION = "member-v1";

    public static String encode(String scope, Object value, Long id) {
        return CursorCodec.encode(
                VERSION, CursorCodec.encodeText(scope), String.valueOf(value), String.valueOf(id));
    }

    public static MemberHistoryCursor decode(String cursor, String scope) {
        try {
            String[] parts = CursorCodec.decode(cursor, 4);
            if (!parts[0].equals(VERSION) || !parts[1].equals(CursorCodec.encodeText(scope))) {
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

    private static HappyGalleryException invalidCursor() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT,
                "검색 조건이 바뀌었거나 페이지 커서가 올바르지 않습니다. 처음부터 조회해 주세요.");
    }
}
