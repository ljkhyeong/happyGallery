package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ModerationActionItem;
import com.personal.happygallery.domain.review.ReviewModerationActionType;
import com.personal.happygallery.domain.review.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReviewModerationActionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reviewId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewModerationActionType.class)
        ReviewModerationActionType action,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewStatus.class)
        ReviewStatus previousStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewStatus.class)
        ReviewStatus newStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long adminUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        ReviewEvidenceResponse evidence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static ReviewModerationActionResponse from(ModerationActionItem action) {
        return new ReviewModerationActionResponse(
                action.id(),
                action.reviewId(),
                action.action(),
                action.previousStatus(),
                action.newStatus(),
                action.reason(),
                action.adminUserId(),
                ReviewEvidenceResponse.from(action.evidence()),
                action.createdAt());
    }
}
