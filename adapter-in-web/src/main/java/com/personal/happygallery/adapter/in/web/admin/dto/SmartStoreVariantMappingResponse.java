package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import io.swagger.v3.oas.annotations.media.Schema;

public record SmartStoreVariantMappingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long optionId
) {
    public static SmartStoreVariantMappingResponse from(VariantMapping mapping) {
        return new SmartStoreVariantMappingResponse(mapping.productVariantId(), mapping.optionId());
    }
}
