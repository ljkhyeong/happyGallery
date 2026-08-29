package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreProductNoticeUseCase.NoticePage;
import com.personal.happygallery.application.product.port.in.SmartStoreProductNoticeUseCase.NoticeSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

public record SmartStoreNoticePageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<NoticeSummaryResponse> notices,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages
) {
    public static SmartStoreNoticePageResponse from(NoticePage page) {
        return new SmartStoreNoticePageResponse(
                page.notices().stream().map(NoticeSummaryResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public record NoticeSummaryResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sellerNoticeId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String postCategoryType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean importantNotice,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime importantNoticeStartDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime importantNoticeEndDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean wholeNotice,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime displayStartDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime displayEndDate
    ) {
        private static NoticeSummaryResponse from(NoticeSummary notice) {
            return new NoticeSummaryResponse(
                    notice.sellerNoticeId(), notice.postCategoryType(), notice.title(),
                    notice.importantNotice(), notice.importantNoticeStartDate(),
                    notice.importantNoticeEndDate(), notice.wholeNotice(),
                    notice.displayStartDate(), notice.displayEndDate());
        }
    }
}
