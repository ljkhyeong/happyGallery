package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.RatingHistogram;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewRatingHistogramResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long rating1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long rating2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long rating3,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long rating4,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long rating5
) {
    public static ReviewRatingHistogramResponse from(RatingHistogram histogram) {
        return new ReviewRatingHistogramResponse(
                histogram.rating1(),
                histogram.rating2(),
                histogram.rating3(),
                histogram.rating4(),
                histogram.rating5());
    }
}
