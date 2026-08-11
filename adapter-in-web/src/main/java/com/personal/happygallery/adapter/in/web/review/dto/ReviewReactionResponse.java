package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReaction;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewReactionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reviewId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean helpfulByMe,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean reportedByMe,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean ownedByMe,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean canInteract
) {
    public static ReviewReactionResponse from(ReviewReaction reaction) {
        return new ReviewReactionResponse(
                reaction.reviewId(),
                reaction.helpfulByMe(),
                reaction.reportedByMe(),
                reaction.ownedByMe(),
                reaction.canInteract());
    }
}
