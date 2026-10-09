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

    /** 첫 페이지(null)는 값이 모두 null인 커서로 바꿔 nullable 커서 조회에 그대로 넘긴다. */
    static CursorParam decodeOrFirst(String cursor) {
        return cursor == null ? CursorParam.FIRST : decode(cursor);
    }

    private static CursorParam decode(String cursor) {
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

    /** 값이 모두 null이면 첫 페이지를 뜻한다. */
    record CursorParam(
            LocalDateTime completedAt,
            ReviewTargetType targetType,
            Long sourceId
    ) {
        private static final CursorParam FIRST = new CursorParam(null, null, null);
    }
}
