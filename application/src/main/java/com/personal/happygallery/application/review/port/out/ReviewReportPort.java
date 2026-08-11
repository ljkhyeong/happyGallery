package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewReportPort {

    ReviewReport save(ReviewReport report);

    Optional<ReviewReport> findByIdForUpdate(Long reportId);

    boolean existsByReviewIdAndReporterUserId(Long reviewId, Long reporterUserId);

    List<ReviewReport> findForAdmin(ReviewReportStatus status, int limit);

    List<ReviewReport> findForAdminAfter(
            ReviewReportStatus status, LocalDateTime createdAt, Long id, int limit);

    List<Long> findReportedReviewIds(Long userId, List<Long> reviewIds);

    List<ReviewReport> findResolvedBefore(LocalDateTime cutoff, int limit);

    void deleteAll(List<ReviewReport> reports);
}
