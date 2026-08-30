package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.domain.review.ReviewReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideReviewReportRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewReportDecision.class)
        @NotNull ReviewReportDecision decision,
        @Schema(
                nullable = true,
                maxLength = ReviewReport.MAX_DECISION_NOTE_LENGTH)
        @Size(max = ReviewReport.MAX_DECISION_NOTE_LENGTH) String note
) {
}
