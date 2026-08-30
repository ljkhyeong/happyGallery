package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MemberReviewReportResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reviewId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportReason.class)
        ReviewReportReason reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportStatus.class)
        ReviewReportStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static MemberReviewReportResponse from(ReviewReportItem report) {
        return new MemberReviewReportResponse(
                report.id(),
                report.reviewId(),
                report.reason(),
                report.detail(),
                report.status(),
                report.createdAt());
    }
}
