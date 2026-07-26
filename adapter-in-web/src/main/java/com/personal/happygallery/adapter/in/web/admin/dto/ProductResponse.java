package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductResponse(
        Long id,
        String name,
        String type,
        String category,
        long price,
        String description,
        String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String specification,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String careInstructions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Integer productionLeadDays,
        String status,
        boolean available,
        int quantity
) {
    public static ProductResponse from(ProductQueryUseCase.ProductWithInventory r) {
        return from(r.product(), r.inventory());
    }

    public static ProductResponse from(ProductAdminUseCase.ProductInventoryResult r) {
        return from(r.product(), r.inventory());
    }

    private static ProductResponse from(Product product, Inventory inventory) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getType().name(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays(),
                product.getStatus().name(),
                product.getStatus() == ProductStatus.ACTIVE && inventory.isAvailable(),
                inventory.getQuantity()
        );
    }
}
