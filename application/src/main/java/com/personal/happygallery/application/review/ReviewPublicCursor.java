package com.personal.happygallery.application.review;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.application.shared.page.CursorCodec;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** 정렬 기준과 별점을 포함해 다른 공개 정렬에 재사용되지 않는 후기 전용 커서. */
final class ReviewPublicCursor {

    private static final String VERSION = "review-v1";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ReviewPublicCursor() {}

    static String encode(
            ReviewSort sort,
            Integer ratingFilter,
            int rating,
            LocalDateTime createdAt,
            Long id) {
        return CursorCodec.encode(
                VERSION,
                sort.name(),
                Objects.toString(ratingFilter, "ALL"),
                Integer.toString(rating),
                FORMATTER.format(createdAt),
                id.toString());
    }

    static CursorParam decode(
            String cursor, ReviewSort expectedSort, Integer expectedRatingFilter) {
        try {
            String[] parts = CursorCodec.decode(cursor, 6);
            if (!VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Invalid review cursor");
            }
            ReviewSort sort = ReviewSort.valueOf(parts[1]);
            Integer ratingFilter = "ALL".equals(parts[2])
                    ? null
                    : Integer.valueOf(parts[2]);
            int rating = Integer.parseInt(parts[3]);
            LocalDateTime createdAt = LocalDateTime.parse(parts[4], FORMATTER);
            Long id = Long.valueOf(parts[5]);
            if (sort != expectedSort
                    || !Objects.equals(ratingFilter, expectedRatingFilter)
                    || rating < 1
                    || rating > 5
                    || id < 1L) {
                throw new IllegalArgumentException("Mismatched review cursor");
            }
            return new CursorParam(rating, createdAt, id);
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "후기 페이지 커서가 올바르지 않습니다.");
        }
    }

    record CursorParam(int rating, LocalDateTime createdAt, Long id) {}
}
