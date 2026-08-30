package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.OfficialReplyItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminOfficialReviewReplyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long adminUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime editedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited
) {
    public static AdminOfficialReviewReplyResponse from(OfficialReplyItem reply) {
        if (reply == null) {
            return null;
        }
        return new AdminOfficialReviewReplyResponse(
                reply.content(),
                reply.adminUserId(),
                reply.createdAt(),
                reply.editedAt(),
                reply.editedAt() != null);
    }
}
