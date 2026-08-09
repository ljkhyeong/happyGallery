package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.HelpfulResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewHelpfulResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reviewId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long helpfulCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean helpfulByMe
) {
    public static ReviewHelpfulResponse from(HelpfulResult result) {
        return new ReviewHelpfulResponse(
                result.reviewId(), result.helpfulCount(), result.helpfulByMe());
    }
}
