package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingHistoryResult;
import com.personal.happygallery.domain.product.SmartStoreInventoryMappingAction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreInventoryMappingHistoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        SmartStoreInventoryMappingAction action,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long previousOriginProductNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long nextOriginProductNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Boolean previousEnabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Boolean nextEnabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String previousOptionMappings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextOptionMappings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long previousMappingVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long nextMappingVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean previousOriginConfirmed,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long changedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String changedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime changedAt
) {
    public static SmartStoreInventoryMappingHistoryResponse from(MappingHistoryResult result) {
        return new SmartStoreInventoryMappingHistoryResponse(
                result.id(),
                result.action(),
                result.previousOriginProductNo(),
                result.nextOriginProductNo(),
                result.previousEnabled(),
                result.nextEnabled(),
                result.previousOptionMappings(),
                result.nextOptionMappings(),
                result.previousMappingVersion(),
                result.nextMappingVersion(),
                result.previousOriginConfirmed(),
                result.changedByAdminId(),
                result.changedBy(),
                result.changedAt());
    }
}
