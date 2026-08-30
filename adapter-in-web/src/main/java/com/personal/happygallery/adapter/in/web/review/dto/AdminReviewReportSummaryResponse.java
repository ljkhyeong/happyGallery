package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportSummaryItem;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminReviewReportSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reviewId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportReason.class)
        ReviewReportReason reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewStatus.class)
        ReviewStatus snapshotStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportStatus.class)
        ReviewReportStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static AdminReviewReportSummaryResponse from(ReviewReportSummaryItem report) {
        return new AdminReviewReportSummaryResponse(
                report.id(),
                report.reviewId(),
                report.reason(),
                report.snapshotStatus(),
                report.status(),
                report.createdAt());
    }
}
