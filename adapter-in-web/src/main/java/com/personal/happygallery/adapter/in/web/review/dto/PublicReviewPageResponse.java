package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.PublicReviewPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PublicReviewPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReviewSummaryResponse summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long filteredCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PublicReviewResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {
    public static PublicReviewPageResponse from(PublicReviewPage page) {
        return new PublicReviewPageResponse(
                ReviewSummaryResponse.from(page.summary()),
                page.filteredCount(),
                page.reviews().content().stream().map(PublicReviewResponse::from).toList(),
                page.reviews().nextCursor(),
                page.reviews().hasMore());
    }
}
