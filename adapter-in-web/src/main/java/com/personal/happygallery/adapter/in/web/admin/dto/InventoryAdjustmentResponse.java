package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record InventoryAdjustmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) InventoryAdjustmentType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantityBefore,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantityAfter,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long adjustedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String adjustedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime adjustedAt
) {
    public static InventoryAdjustmentResponse from(InventoryAdjustment adjustment) {
        return new InventoryAdjustmentResponse(
                adjustment.getId(),
                adjustment.getProductId(),
                adjustment.getProductVariantId(),
                adjustment.getType(),
                adjustment.getQuantity(),
                adjustment.getQuantityBefore(),
                adjustment.getQuantityAfter(),
                adjustment.getReason(),
                adjustment.getAdjustedByAdminId(),
                adjustment.getAdjustedBy(),
                adjustment.getAdjustedAt());
    }
}
