package com.personal.happygallery.application.review;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

final class ReviewOpportunityCursor {

    private static final String SEPARATOR = "|";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ReviewOpportunityCursor() {}

    static String encode(LocalDateTime completedAt, ReviewTargetType targetType, Long sourceId) {
        String raw = FORMATTER.format(completedAt)
                + SEPARATOR + targetType.name()
                + SEPARATOR + sourceId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static CursorParam decode(String cursor) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid opportunity cursor");
            }
            LocalDateTime completedAt = LocalDateTime.parse(parts[0], FORMATTER);
            ReviewTargetType targetType = ReviewTargetType.valueOf(parts[1]);
            Long sourceId = Long.valueOf(parts[2]);
            if (sourceId < 1L) {
                throw new IllegalArgumentException("Invalid source id");
            }
            return new CursorParam(completedAt, targetType, sourceId);
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "후기 작성 기회 커서가 올바르지 않습니다.");
        }
    }

    record CursorParam(
            LocalDateTime completedAt,
            ReviewTargetType targetType,
            Long sourceId
    ) {}
}
