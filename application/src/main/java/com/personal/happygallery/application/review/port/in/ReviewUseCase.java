package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewEvidenceProvenance;
import com.personal.happygallery.domain.review.ReviewModerationActionType;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.List;

/** 후기 계층 간 응답 모델. 실행 포트는 역할별 인터페이스로 분리한다. */
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
            long contentRevision,
            long version,
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
            ReviewEvidenceItem evidence,
            LocalDateTime createdAt
    ) {}

    record ReviewEvidenceItem(
            Long id,
            long contentRevision,
            int rating,
            String content,
            LocalDateTime editedAt,
            ReviewEvidenceProvenance provenance,
            boolean imagesComplete,
            List<String> imageUrls,
            LocalDateTime capturedAt
    ) {
        public ReviewEvidenceItem {
            imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        }
    }

    record ReviewReportItem(
            Long id,
            Long reviewId,
            Long reporterUserId,
            ReviewReportReason reason,
            String detail,
            ReviewStatus snapshotStatus,
            ReviewEvidenceItem evidence,
            ReviewReportStatus status,
            String decisionNote,
            Long decidedByAdminId,
            LocalDateTime decidedAt,
            LocalDateTime createdAt
    ) {}

    record ReviewReportSummaryItem(
            Long id,
            Long reviewId,
            ReviewReportReason reason,
            ReviewStatus snapshotStatus,
            ReviewReportStatus status,
            LocalDateTime createdAt
    ) {}

    record ReviewReaction(
            Long reviewId,
            boolean helpfulByMe,
            boolean reportedByMe,
            boolean ownedByMe,
            boolean canInteract
    ) {}

    record HelpfulResult(
            Long reviewId,
            long helpfulCount,
            boolean helpfulByMe
    ) {}

}
