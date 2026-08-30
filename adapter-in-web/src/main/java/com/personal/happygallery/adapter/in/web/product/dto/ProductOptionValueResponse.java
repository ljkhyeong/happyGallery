package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.ProductOptions.OptionValue;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductOptionValueResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder
) {
    public static ProductOptionValueResponse from(OptionValue value) {
        return new ProductOptionValueResponse(value.key(), value.name(), value.sortOrder());
    }
}
