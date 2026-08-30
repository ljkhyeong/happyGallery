package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.ProductOptions.OptionGroup;
import com.personal.happygallery.domain.product.ProductOptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductOptionGroupResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductOptionType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String inputPlaceholder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Integer inputMaxLength,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long inputPriceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProductOptionValueResponse> values
) {
    public static ProductOptionGroupResponse from(OptionGroup group) {
        return new ProductOptionGroupResponse(
                group.key(), group.type(), group.name(), group.required(), group.sortOrder(),
                group.inputPlaceholder(), group.inputMaxLength(), group.inputPriceAdjustment(),
                group.values().stream().map(ProductOptionValueResponse::from).toList());
    }
}
