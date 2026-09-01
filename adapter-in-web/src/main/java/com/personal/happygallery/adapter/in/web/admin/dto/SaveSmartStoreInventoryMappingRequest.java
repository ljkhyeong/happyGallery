package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveSmartStoreInventoryMappingRequest(
        @NotNull @Positive Long originProductNo,
        boolean enabled,
        @Size(max = ProductOptionPolicy.MAX_COMBINATIONS)
        List<@NotNull @Valid SmartStoreVariantMappingRequest> variants,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        @Positive Long expectedMappingVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean previousOriginConfirmed
) {
    public SaveSmartStoreInventoryMappingRequest {
        variants = variants == null ? List.of() : List.copyOf(variants);
    }

    public SaveMappingCommand toCommand() {
        return new SaveMappingCommand(
                originProductNo,
                enabled,
                variants.stream().map(SmartStoreVariantMappingRequest::toCommand).toList(),
                expectedMappingVersion,
                previousOriginConfirmed);
    }
}
