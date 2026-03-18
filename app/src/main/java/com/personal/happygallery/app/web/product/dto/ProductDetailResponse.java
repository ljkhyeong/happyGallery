package com.personal.happygallery.app.web.product.dto;

import com.personal.happygallery.app.product.ProductQueryService;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;

public record ProductDetailResponse(
        Long id,
        String name,
        String type,
        long price,
        boolean available
) {
    public static ProductDetailResponse from(ProductQueryService.ProductWithInventory r) {
        return from(r.product(), r.inventory());
    }

    private static ProductDetailResponse from(Product product, Inventory inventory) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getType().name(),
                product.getPrice(),
                inventory.isAvailable()
        );
    }
}
