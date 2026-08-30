package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.ProductOptions.Selection;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductVariantSelectionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String groupKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String valueKey
) {
    public static ProductVariantSelectionResponse from(Selection selection) {
        return new ProductVariantSelectionResponse(selection.groupKey(), selection.valueKey());
    }
}
