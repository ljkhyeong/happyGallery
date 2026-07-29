package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.shared.page.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminBookingSearchPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<AdminBookingSearchItemResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPages
) {

    public static AdminBookingSearchPageResponse from(
            OffsetPage<AdminBookingSearchRow> result) {
        return new AdminBookingSearchPageResponse(
                result.content().stream().map(AdminBookingSearchItemResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalCount(),
                result.totalPages());
    }
}
