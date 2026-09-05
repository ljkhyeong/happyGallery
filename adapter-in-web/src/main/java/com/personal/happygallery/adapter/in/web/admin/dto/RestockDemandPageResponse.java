package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.RestockDemandUseCase.Demand;
import com.personal.happygallery.application.shared.page.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RestockDemandPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestockDemandResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPages) {
    public record RestockDemandResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String optionLabel,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long waitingCount) {}
    public static RestockDemandPageResponse from(OffsetPage<Demand> page) {
        return new RestockDemandPageResponse(page.content().stream().map(row -> new RestockDemandResponse(
                row.productId(), row.productName(), row.productVariantId(), row.optionLabel(), row.waitingCount())).toList(),
                page.page(), page.size(), page.totalCount(), page.totalPages());
    }
}
