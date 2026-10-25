package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewStatus;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaReviewReportAdapterTest {

    @DisplayName("후기와 신고자 유일 제약 충돌만 중복 신고 오류로 번역한다")
    @Test
    void save_duplicateReporter_mapsToReviewReportAlreadyExists() {
        ReviewReportRepository repository = mock(ReviewReportRepository.class);
        ReviewReport report = report();
        when(repository.saveAndFlush(report))
                .thenThrow(constraintViolation("review_reports.uq_review_reports_review_reporter"));
        JpaReviewReportAdapter adapter = new JpaReviewReportAdapter(repository);

        assertThatThrownBy(() -> adapter.save(report))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS);
    }

    @DisplayName("후기 신고 저장의 다른 무결성 오류는 중복 신고로 숨기지 않는다")
    @Test
    void save_otherConstraint_propagatesOriginalFailure() {
        ReviewReportRepository repository = mock(ReviewReportRepository.class);
        ReviewReport report = report();
        DataIntegrityViolationException failure = constraintViolation("fk_review_reports_review");
        when(repository.saveAndFlush(report)).thenThrow(failure);
        JpaReviewReportAdapter adapter = new JpaReviewReportAdapter(repository);

        assertThatThrownBy(() -> adapter.save(report)).isSameAs(failure);
    }

    private static ReviewReport report() {
        return new ReviewReport(
                1L,
                2L,
                ReviewReportReason.SPAM,
                null,
                ReviewStatus.PUBLISHED,
                3L,
                LocalDateTime.of(2026, 8, 9, 12, 0));
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "DB constraint violation",
                new ConstraintViolationException(
                        "Constraint violation", new SQLException(), constraintName));
    }
}
