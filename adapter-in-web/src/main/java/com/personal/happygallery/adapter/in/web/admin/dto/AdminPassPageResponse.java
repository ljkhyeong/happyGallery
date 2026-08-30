package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.application.shared.page.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminPassPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminPassResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPages
) {

    public static AdminPassPageResponse from(OffsetPage<AdminPassView> page) {
        return new AdminPassPageResponse(
                page.content().stream().map(AdminPassResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalCount(),
                page.totalPages());
    }
}
