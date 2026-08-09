package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.domain.review.ReviewImage;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record AdminReviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String authorName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewTargetType.class)
        ReviewTargetType targetType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long targetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String targetName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewSourceType.class)
        ReviewSourceType sourceType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "5")
        int rating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewStatus.class)
        ReviewStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String hiddenReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime hiddenAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long hiddenByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime editedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean verifiedTransaction,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        AdminOfficialReviewReplyResponse officialReply,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long helpfulCount,
        @ArraySchema(
                arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = ReviewImageResponse.class),
                maxItems = ReviewImage.MAX_IMAGES)
        List<ReviewImageResponse> images
) {
    public static AdminReviewResponse from(ReviewItem item) {
        return new AdminReviewResponse(
                item.id(),
                item.userId(),
                item.authorName(),
                item.targetType(),
                item.targetId(),
                item.targetName(),
                ReviewSourceType.from(item.targetType()),
                item.sourceId(),
                item.rating(),
                item.content(),
                item.status(),
                item.hiddenReason(),
                item.hiddenAt(),
                item.hiddenByAdminId(),
                item.createdAt(),
                item.updatedAt(),
                item.edited(),
                item.editedAt(),
                item.verifiedTransaction(),
                AdminOfficialReviewReplyResponse.from(item.officialReply()),
                item.helpfulCount(),
                item.images().stream().map(ReviewImageResponse::from).toList());
    }
}
