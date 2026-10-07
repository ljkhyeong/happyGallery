package com.personal.happygallery.application.shared.page;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 커서 인코딩/디코딩 유틸리티.
 *
 * <p>커서 형식: Base64("{ISO_LOCAL_DATE_TIME}|{key}")
 * <br>동점 방지를 위해 (timestamp, key) 복합 키를 사용한다. key는 숫자 ID 또는 문자열 키다.
 */
public final class CursorUtils {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CursorUtils() {}

    public static String encode(LocalDateTime timestamp, Long id) {
        return encode(timestamp, String.valueOf(id));
    }

    /** 문자열 키(예: 스마트스토어 상품 주문 번호)로 동점을 가르는 커서를 만든다. */
    public static String encode(LocalDateTime timestamp, String key) {
        return CursorCodec.encode(FMT.format(timestamp), key);
    }

    public static CursorParam decode(String cursor) {
        KeyCursor keyCursor = decodeKey(cursor);
        try {
            return new CursorParam(keyCursor.timestamp(), Long.valueOf(keyCursor.key()));
        } catch (NumberFormatException exception) {
            throw invalidCursor();
        }
    }

    /** 첫 페이지(null)는 값이 모두 null인 커서로 바꿔 nullable 커서 조회에 그대로 넘긴다. */
    public static CursorParam decodeOrFirst(String cursor) {
        return cursor == null ? CursorParam.FIRST : decode(cursor);
    }

    public static KeyCursor decodeKey(String cursor) {
        try {
            String[] values = CursorCodec.decode(cursor, 2);
            if (values[1].isEmpty()) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
            return new KeyCursor(LocalDateTime.parse(values[0], FMT), values[1]);
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private static HappyGalleryException invalidCursor() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, "페이지 커서가 올바르지 않습니다.");
    }

    /** 값이 모두 null이면 첫 페이지를 뜻한다. */
    public record CursorParam(LocalDateTime timestamp, Long id) {
        private static final CursorParam FIRST = new CursorParam(null, null);
    }

    public record KeyCursor(LocalDateTime timestamp, String key) {}
}
