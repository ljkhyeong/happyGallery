package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;

public record ProductDetailResponse(
        Long id,
        String name,
        String type,
        String category,
        long price,
        boolean available
) {
    public static ProductDetailResponse from(ProductQueryUseCase.ProductWithInventory r) {
        return from(r.product(), r.inventory());
    }

    private static ProductDetailResponse from(Product product, Inventory inventory) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getType().name(),
                product.getCategory(),
                product.getPrice(),
                inventory.isAvailable()
        );
    }
}
