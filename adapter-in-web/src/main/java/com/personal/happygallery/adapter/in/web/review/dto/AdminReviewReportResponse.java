package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminReviewReportResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reviewId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reporterUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportReason.class)
        ReviewReportReason reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "5")
        int snapshotRating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String snapshotContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewStatus.class)
        ReviewStatus snapshotStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime snapshotEditedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportStatus.class)
        ReviewReportStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String decisionNote,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long decidedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime decidedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static AdminReviewReportResponse from(ReviewReportItem report) {
        return new AdminReviewReportResponse(
                report.id(),
                report.reviewId(),
                report.reporterUserId(),
                report.reason(),
                report.detail(),
                report.snapshotRating(),
                report.snapshotContent(),
                report.snapshotStatus(),
                report.snapshotEditedAt(),
                report.status(),
                report.decisionNote(),
                report.decidedByAdminId(),
                report.decidedAt(),
                report.createdAt());
    }
}
