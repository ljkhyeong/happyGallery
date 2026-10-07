package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.review.ReviewSort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewReaderPort {

    Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

    Optional<Review> findByIdAndUserIdForUpdate(Long reviewId, Long userId);

    Optional<Review> findByIdForUpdate(Long reviewId);

    Optional<ReviewListView> findViewById(Long reviewId);

    List<ReviewInteractionStateView> findInteractionStates(List<Long> reviewIds);

    List<ReviewListView> findPublishedByProduct(
            Long productId,
            Integer rating,
            ReviewSort sort,
            Integer cursorRating,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit);

    List<ReviewListView> findPublishedByClass(
            Long classId,
            Integer rating,
            ReviewSort sort,
            Integer cursorRating,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit);

    ReviewSummaryView summarizePublishedProduct(Long productId);

    ReviewSummaryView summarizePublishedClass(Long classId);

    long countPublishedProduct(Long productId, Integer rating);

    long countPublishedClass(Long classId, Integer rating);

    /** 커서 시각·ID가 null이면 첫 페이지를 조회한다. */
    List<ReviewListView> findByUserId(
            Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    List<ReviewListView> findByOwnedOrder(Long userId, Long orderId);

    List<ReviewListView> findByOwnedBooking(Long userId, Long bookingId);

    /** 커서 시각·ID가 null이면 첫 페이지를 조회한다. */
    List<ReviewListView> findForAdmin(
            ReviewTargetType targetType,
            ReviewStatus status,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit);
}
