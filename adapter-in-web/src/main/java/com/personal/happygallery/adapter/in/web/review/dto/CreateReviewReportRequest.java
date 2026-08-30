package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewReportRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportReason.class)
        @NotNull ReviewReportReason reason,
        @Schema(
                nullable = true,
                maxLength = ReviewReport.MAX_DETAIL_LENGTH)
        @Size(max = ReviewReport.MAX_DETAIL_LENGTH) String detail
) {
}
