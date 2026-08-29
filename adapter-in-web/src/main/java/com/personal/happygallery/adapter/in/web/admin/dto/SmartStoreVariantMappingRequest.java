package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SmartStoreVariantMappingRequest(
        @NotNull @Positive Long productVariantId,
        @NotNull @Positive Long optionId
) {
    public VariantMapping toCommand() {
        return new VariantMapping(productVariantId, optionId);
    }
}
