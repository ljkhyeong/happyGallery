package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.adapter.in.web.MaskingUtil;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.domain.review.ReviewImage;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PublicReviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "5")
        int rating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String authorName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = ReviewSourceType.class)
        ReviewSourceType sourceType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean verifiedTransaction,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime editedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        OfficialReviewReplyResponse officialReply,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long helpfulCount,
        @ArraySchema(
                arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = ReviewImageResponse.class),
                maxItems = ReviewImage.MAX_IMAGES)
        List<ReviewImageResponse> images
) {
    public static PublicReviewResponse from(ReviewItem item) {
        return new PublicReviewResponse(
                item.id(),
                item.rating(),
                item.content(),
                MaskingUtil.maskName(item.authorName()),
                ReviewSourceType.from(item.targetType()),
                item.verifiedTransaction(),
                item.createdAt(),
                item.updatedAt(),
                item.edited(),
                item.editedAt(),
                OfficialReviewReplyResponse.from(item.officialReply()),
                item.helpfulCount(),
                item.images().stream().map(ReviewImageResponse::from).toList());
    }
}
