package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewOpportunity;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ReviewOpportunityPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ReviewOpportunityResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {
    public static ReviewOpportunityPageResponse from(CursorPage<ReviewOpportunity> page) {
        return new ReviewOpportunityPageResponse(
                page.content().stream().map(ReviewOpportunityResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
