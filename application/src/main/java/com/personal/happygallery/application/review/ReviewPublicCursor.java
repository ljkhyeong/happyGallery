package com.personal.happygallery.application.review;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.ReviewSort;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;

/** 정렬 기준과 별점을 포함해 다른 공개 정렬에 재사용되지 않는 후기 전용 커서. */
final class ReviewPublicCursor {

    private static final String VERSION = "review-v1";
    private static final String SEPARATOR = "|";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ReviewPublicCursor() {}

    static String encode(
            ReviewSort sort,
            Integer ratingFilter,
            int rating,
            LocalDateTime createdAt,
            Long id) {
        String raw = String.join(
                SEPARATOR,
                VERSION,
                sort.name(),
                ratingFilter == null ? "ALL" : ratingFilter.toString(),
                Integer.toString(rating),
                FORMATTER.format(createdAt),
                id.toString());
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static CursorParam decode(
            String cursor, ReviewSort expectedSort, Integer expectedRatingFilter) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 6 || !VERSION.equals(parts[0])) {
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
