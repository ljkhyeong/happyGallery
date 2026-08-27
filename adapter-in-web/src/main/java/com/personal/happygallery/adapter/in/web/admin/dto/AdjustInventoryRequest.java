package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdjustInventoryRequest(
        @Positive @Schema(nullable = true) Long productVariantId,
        @NotNull InventoryAdjustmentType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive int quantity,
        @NotBlank @Size(max = 500) String reason
) {
    public AdjustInventoryRequest(InventoryAdjustmentType type, int quantity, String reason) {
        this(null, type, quantity, reason);
    }
}
