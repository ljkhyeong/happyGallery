package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ModerationActionItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportSummaryItem;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.util.List;

/** 후기 공개 상태·답글·신고를 관리하는 관리자 유스케이스. */
public interface AdminReviewUseCase {

    CursorPage<ReviewItem> listAdminReviews(
            ReviewTargetType targetType, ReviewStatus status, String cursor, int size);

    ReviewItem getAdminReview(Long reviewId);

    ReviewItem updateStatus(
            Long reviewId,
            ReviewStatus status,
            String reason,
            long expectedContentRevision,
            long expectedVersion,
            Long adminUserId);

    List<ModerationActionItem> listModerationActions(Long reviewId);

    ReviewItem upsertOfficialReply(
            Long reviewId, String content, long expectedVersion, Long adminUserId);

    ReviewItem deleteOfficialReply(Long reviewId, long expectedVersion);

    CursorPage<ReviewReportSummaryItem> listAdminReports(
            ReviewReportStatus status, String cursor, int size);

    ReviewReportItem getAdminReport(Long reportId);

    ReviewReportItem decideReport(
            Long reportId,
            ReviewReportStatus decision,
            String decisionNote,
            Long adminUserId);
}
