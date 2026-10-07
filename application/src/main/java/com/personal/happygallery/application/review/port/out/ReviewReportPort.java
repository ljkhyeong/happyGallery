package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportSummaryItem;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewReportPort {

    ReviewReport save(ReviewReport report);

    Optional<ReviewReport> findByIdForUpdate(Long reportId);

    Optional<ReviewReport> findById(Long reportId);

    boolean existsByReviewIdAndReporterUserId(Long reviewId, Long reporterUserId);

    /** 관리자 신고 목록은 민감 상세와 증거를 제외한다. 커서가 null이면 첫 페이지를 조회한다. */
    List<ReviewReportSummaryItem> findForAdmin(
            ReviewReportStatus status, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    List<Long> findReportedReviewIds(Long userId, List<Long> reviewIds);

    List<ReviewReport> findResolvedBefore(LocalDateTime cutoff, int limit);

    void deleteAll(List<ReviewReport> reports);
}
