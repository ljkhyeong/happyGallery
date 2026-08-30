package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewCreationState;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewCreationStateResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewTargetType.class)
        ReviewTargetType targetType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewSourceType.class)
        ReviewSourceType sourceType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewCreationStatus.class)
        ReviewCreationStatus status
) {
    public static ReviewCreationStateResponse from(ReviewCreationState state) {
        return new ReviewCreationStateResponse(
                state.targetType(),
                ReviewSourceType.from(state.targetType()),
                state.sourceId(),
                state.status());
    }
}
