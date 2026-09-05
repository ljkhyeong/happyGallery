package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.StockLevel;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

public record StockLevelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Integer minimumStock,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean lowStock) {
    public static StockLevelResponse from(StockLevel row) {
        return new StockLevelResponse(row.productId(), row.productVariantId(), row.productName(), row.type(),
                row.quantity(), row.minimumStock(), row.version(), row.active(), row.lowStock());
    }
}
