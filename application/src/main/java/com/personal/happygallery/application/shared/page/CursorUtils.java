package com.personal.happygallery.application.shared.page;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 커서 인코딩/디코딩 유틸리티.
 *
 * <p>커서 형식: Base64("{ISO_LOCAL_DATE_TIME}|{id}")
 * <br>동점 방지를 위해 (timestamp, id) 복합 키를 사용한다.
 */
public final class CursorUtils {

    private static final String SEPARATOR = "|";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CursorUtils() {}

    public static String encode(LocalDateTime timestamp, Long id) {
        String raw = FMT.format(timestamp) + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorParam decode(String cursor) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf(SEPARATOR);
            if (sep < 0) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
            LocalDateTime timestamp = LocalDateTime.parse(raw.substring(0, sep), FMT);
            Long id = Long.valueOf(raw.substring(sep + 1));
            return new CursorParam(timestamp, id);
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "페이지 커서가 올바르지 않습니다.");
        }
    }

    public record CursorParam(LocalDateTime timestamp, Long id) {}
}
