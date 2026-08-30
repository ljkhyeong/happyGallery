package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.ProductOptions.Variant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductVariantResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long priceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ProductVariantSelectionResponse> selections
) {
    public static ProductVariantResponse from(Variant variant) {
        return new ProductVariantResponse(
                variant.id(), variant.priceAdjustment(), variant.quantity(), variant.active(),
                variant.selections().stream().map(ProductVariantSelectionResponse::from).toList());
    }
}
