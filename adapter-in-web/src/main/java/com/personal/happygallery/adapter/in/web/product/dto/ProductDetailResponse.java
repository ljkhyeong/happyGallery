package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;

public record ProductDetailResponse(
        Long id,
        String name,
        String type,
        String category,
        long price,
        String description,
        String imageUrl,
        boolean available
) {
    public static ProductDetailResponse from(ProductQueryUseCase.ProductWithInventory r) {
        Product product = r.product();
        Inventory inventory = r.inventory();
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getType().name(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus() == ProductStatus.ACTIVE && inventory.isAvailable()
        );
    }
}
