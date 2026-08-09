package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminReviewReportPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminReviewReportResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {
    public static AdminReviewReportPageResponse from(CursorPage<ReviewReportItem> page) {
        return new AdminReviewReportPageResponse(
                page.content().stream().map(AdminReviewReportResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
