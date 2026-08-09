package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewModerationActionType;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.List;

/** 회원·공개·관리자 후기 유스케이스. */
public interface ReviewUseCase {

    record OfficialReplyItem(
            String content,
            Long adminUserId,
            LocalDateTime createdAt,
            LocalDateTime editedAt
    ) {}

    record ReviewImageItem(
            Long id,
            String imageUrl,
            int sortOrder,
            LocalDateTime createdAt
    ) {}

    record ReviewItem(
            Long id,
            Long userId,
            String authorName,
            ReviewTargetType targetType,
            Long sourceId,
            Long targetId,
            String targetName,
            int rating,
            String content,
            ReviewStatus status,
            String hiddenReason,
            LocalDateTime hiddenAt,
            Long hiddenByAdminId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime editedAt,
            boolean edited,
            boolean verifiedTransaction,
            OfficialReplyItem officialReply,
            long helpfulCount,
            List<ReviewImageItem> images
    ) {
        public ReviewItem {
            images = images == null ? List.of() : List.copyOf(images);
        }
    }

    record RatingHistogram(
            long rating1,
            long rating2,
            long rating3,
            long rating4,
            long rating5
    ) {
        public static RatingHistogram empty() {
            return new RatingHistogram(0L, 0L, 0L, 0L, 0L);
        }
    }

    record ReviewSummary(
            long reviewCount,
            double averageRating,
            RatingHistogram histogram
    ) {
        public ReviewSummary(long reviewCount, double averageRating) {
            this(reviewCount, averageRating, RatingHistogram.empty());
        }
    }

    record PublicReviewPage(
            ReviewSummary summary,
            long filteredCount,
            CursorPage<ReviewItem> reviews
    ) {
        public PublicReviewPage(ReviewSummary summary, CursorPage<ReviewItem> reviews) {
            this(summary, summary.reviewCount(), reviews);
        }
    }

    record ReviewOpportunity(
            ReviewTargetType targetType,
            Long sourceId,
            Long targetId,
            String targetName,
            Long orderId,
            Long bookingId,
            LocalDateTime completedAt
    ) {}

    record ReviewCreationState(
            ReviewTargetType targetType,
            Long sourceId,
            ReviewCreationStatus status
    ) {}

    record ModerationActionItem(
            Long id,
            Long reviewId,
            ReviewModerationActionType action,
            ReviewStatus previousStatus,
            ReviewStatus newStatus,
            String reason,
            Long adminUserId,
            LocalDateTime createdAt
    ) {}

    record ReviewReportItem(
            Long id,
            Long reviewId,
            Long reporterUserId,
            ReviewReportReason reason,
            String detail,
            int snapshotRating,
            String snapshotContent,
            ReviewStatus snapshotStatus,
            LocalDateTime snapshotEditedAt,
            ReviewReportStatus status,
            String decisionNote,
            Long decidedByAdminId,
            LocalDateTime decidedAt,
            LocalDateTime createdAt
    ) {}

    record ReviewReaction(
            Long reviewId,
            boolean helpfulByMe,
            boolean reportedByMe
    ) {}

    record HelpfulResult(
            Long reviewId,
            long helpfulCount,
            boolean helpfulByMe
    ) {}

    ReviewItem createProductReview(
            Long userId, Long orderItemId, int rating, String content);

    ReviewItem createClassReview(
            Long userId, Long bookingId, int rating, String content);

    ReviewItem updateReview(
            Long userId, Long reviewId, int rating, String content);

    void deleteReview(Long userId, Long reviewId);

    default PublicReviewPage listProductReviews(Long productId, String cursor, int size) {
        return listProductReviews(productId, null, ReviewSort.LATEST, cursor, size);
    }

    PublicReviewPage listProductReviews(
            Long productId, Integer rating, ReviewSort sort, String cursor, int size);

    default PublicReviewPage listClassReviews(Long classId, String cursor, int size) {
        return listClassReviews(classId, null, ReviewSort.LATEST, cursor, size);
    }

    PublicReviewPage listClassReviews(
            Long classId, Integer rating, ReviewSort sort, String cursor, int size);

    CursorPage<ReviewItem> listMyReviews(Long userId, String cursor, int size);

    List<ReviewItem> listMyOrderReviews(Long userId, Long orderId);

    List<ReviewItem> listMyBookingReviews(Long userId, Long bookingId);

    List<ReviewOpportunity> listMyReviewOpportunities(Long userId);

    ReviewCreationState getProductReviewCreationState(Long userId, Long orderItemId);

    ReviewCreationState getClassReviewCreationState(Long userId, Long bookingId);

    CursorPage<ReviewItem> listAdminReviews(
            ReviewTargetType targetType, ReviewStatus status, String cursor, int size);

    ReviewItem updateStatus(
            Long reviewId, ReviewStatus status, String reason, Long adminUserId);

    List<ModerationActionItem> listModerationActions(Long reviewId);

    ReviewItem upsertOfficialReply(Long reviewId, String content, Long adminUserId);

    ReviewItem deleteOfficialReply(Long reviewId, Long adminUserId);

    ReviewReportItem createReport(
            Long userId, Long reviewId, ReviewReportReason reason, String detail);

    CursorPage<ReviewReportItem> listAdminReports(
            ReviewReportStatus status, String cursor, int size);

    ReviewReportItem decideReport(
            Long reportId,
            ReviewReportStatus decision,
            String decisionNote,
            Long adminUserId);

    HelpfulResult markHelpful(Long userId, Long reviewId);

    HelpfulResult unmarkHelpful(Long userId, Long reviewId);

    List<ReviewReaction> listMyReviewReactions(Long userId, List<Long> reviewIds);

    ReviewImageItem addReviewImage(
            Long userId, Long reviewId, byte[] bytes, String contentType);

    void deleteReviewImage(Long userId, Long reviewId, Long imageId);
}
