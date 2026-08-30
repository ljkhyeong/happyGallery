package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewOpportunity;
import com.personal.happygallery.domain.review.ReviewTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReviewOpportunityResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewTargetType.class)
        ReviewTargetType targetType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewSourceType.class)
        ReviewSourceType sourceType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long targetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String targetName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime completedAt
) {
    public static ReviewOpportunityResponse from(ReviewOpportunity opportunity) {
        return new ReviewOpportunityResponse(
                opportunity.targetType(),
                ReviewSourceType.from(opportunity.targetType()),
                opportunity.sourceId(),
                opportunity.targetId(),
                opportunity.targetName(),
                opportunity.orderId(),
                opportunity.bookingId(),
                opportunity.completedAt());
    }
}
