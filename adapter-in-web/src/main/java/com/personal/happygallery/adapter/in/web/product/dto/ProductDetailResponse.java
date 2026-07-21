package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ProductType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean available
) {
    public static ProductDetailResponse from(ProductQueryUseCase.ProductWithInventory r) {
        Product product = r.product();
        Inventory inventory = r.inventory();
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getType(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus() == ProductStatus.ACTIVE && inventory.isAvailable()
        );
    }
}
