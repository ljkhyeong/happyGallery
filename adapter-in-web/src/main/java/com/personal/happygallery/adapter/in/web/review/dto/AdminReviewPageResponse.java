package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminReviewPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminReviewResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {
    public static AdminReviewPageResponse from(CursorPage<ReviewItem> page) {
        return new AdminReviewPageResponse(
                page.content().stream().map(AdminReviewResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
