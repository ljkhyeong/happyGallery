package com.personal.happygallery.application.review;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.application.shared.page.CursorCodec;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class ReviewOpportunityCursor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ReviewOpportunityCursor() {}

    static String encode(LocalDateTime completedAt, ReviewTargetType targetType, Long sourceId) {
        return CursorCodec.encode(
                FORMATTER.format(completedAt), targetType.name(), String.valueOf(sourceId));
    }

    static CursorParam decode(String cursor) {
        try {
            String[] parts = CursorCodec.decode(cursor, 3);
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
