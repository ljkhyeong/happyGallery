package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingResult;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record SmartStoreInventoryMappingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long mappingVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long originProductNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<SmartStoreVariantMappingResponse> variants,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        SmartStoreStockSyncStatus syncStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int attemptCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String lastError,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime syncedAt
) {
    public static SmartStoreInventoryMappingResponse from(MappingResult result) {
        return new SmartStoreInventoryMappingResponse(
                result.productId(),
                result.mappingVersion(),
                result.originProductNo(),
                result.enabled(),
                result.variants().stream().map(SmartStoreVariantMappingResponse::from).toList(),
                result.syncStatus(),
                result.attemptCount(),
                result.lastError(),
                result.syncedAt());
    }
}
