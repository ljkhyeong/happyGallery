package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String specification,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String careInstructions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Integer productionLeadDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity
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
                product.getType(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays(),
                product.getStatus(),
                product.getStatus() == ProductStatus.ACTIVE && inventory.isAvailable(),
                inventory.getQuantity()
        );
    }
}
