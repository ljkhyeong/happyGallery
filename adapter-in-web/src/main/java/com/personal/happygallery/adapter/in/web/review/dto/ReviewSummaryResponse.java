package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewSummary;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long reviewCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "5")
        double averageRating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ReviewRatingHistogramResponse histogram
) {
    public static ReviewSummaryResponse from(ReviewSummary summary) {
        return new ReviewSummaryResponse(
                summary.reviewCount(),
                summary.averageRating(),
                ReviewRatingHistogramResponse.from(summary.histogram()));
    }
}
