package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

final class SmartStoreOrderCursor {

    private static final String SEPARATOR = "|";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private SmartStoreOrderCursor() {}

    static String encode(LocalDateTime changedAt, String productOrderId) {
        String raw = FORMATTER.format(changedAt) + SEPARATOR + productOrderId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static CursorParam decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = raw.indexOf(SEPARATOR);
            if (separator < 1 || separator == raw.length() - 1) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            return new CursorParam(
                    LocalDateTime.parse(raw.substring(0, separator), FORMATTER),
                    raw.substring(separator + 1));
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "스마트스토어 주문 페이지 커서가 올바르지 않습니다.");
        }
    }

    record CursorParam(LocalDateTime changedAt, String productOrderId) {}
}
