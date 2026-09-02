package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.InventoryResolutionCommand;
import com.personal.happygallery.domain.order.SmartStoreInventoryResolutionAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveSmartStoreInventoryRequest(
        @NotNull Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @NotNull SmartStoreInventoryResolutionAction action,
        @NotBlank @Size(max = 500) String reason,
        @NotBlank String resolutionVersion
) {
    public InventoryResolutionCommand toCommand(String productOrderId) {
        return new InventoryResolutionCommand(
                productOrderId, productId, productVariantId, action, reason, resolutionVersion);
    }
}
