package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewReportPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewReportAdapter implements ReviewReportPort {

    private final ReviewReportRepository repository;

    JpaReviewReportAdapter(ReviewReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReviewReport save(ReviewReport report) {
        try {
            return repository.saveAndFlush(report);
        } catch (DataIntegrityViolationException exception) {
            throw new HappyGalleryException(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS);
        }
    }

    @Override
    public Optional<ReviewReport> findByIdForUpdate(Long reportId) {
        return repository.findByIdForUpdate(reportId);
    }

    @Override
    public boolean existsByReviewIdAndReporterUserId(Long reviewId, Long reporterUserId) {
        return repository.existsByReviewIdAndReporterUserId(reviewId, reporterUserId);
    }

    @Override
    public List<ReviewReport> findForAdmin(ReviewReportStatus status, int limit) {
        return repository.findAdminRows(status, PageRequest.ofSize(limit));
    }

    @Override
    public List<ReviewReport> findForAdminAfter(
            ReviewReportStatus status, LocalDateTime createdAt, Long id, int limit) {
        return repository.findAdminRowsAfter(status, createdAt, id, PageRequest.ofSize(limit));
    }

    @Override
    public List<Long> findReportedReviewIds(Long userId, List<Long> reviewIds) {
        return reviewIds.isEmpty() ? List.of() : repository.findReportedReviewIds(userId, reviewIds);
    }
}
