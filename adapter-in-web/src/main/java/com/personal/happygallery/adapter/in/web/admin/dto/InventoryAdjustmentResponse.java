package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.InventoryAdjustment;
import java.time.LocalDateTime;

public record InventoryAdjustmentResponse(
        Long id,
        Long productId,
        String type,
        int quantity,
        int quantityBefore,
        int quantityAfter,
        String reason,
        Long adjustedByAdminId,
        String adjustedBy,
        LocalDateTime adjustedAt
) {
    public static InventoryAdjustmentResponse from(InventoryAdjustment adjustment) {
        return new InventoryAdjustmentResponse(
                adjustment.getId(),
                adjustment.getProductId(),
                adjustment.getType().name(),
                adjustment.getQuantity(),
                adjustment.getQuantityBefore(),
                adjustment.getQuantityAfter(),
                adjustment.getReason(),
                adjustment.getAdjustedByAdminId(),
                adjustment.getAdjustedBy(),
                adjustment.getAdjustedAt());
    }
}
