package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;

public record ProductResponse(
        Long id,
        String name,
        String type,
        long price,
        String status,
        boolean available,
        int quantity
) {
    public static ProductResponse from(Product product, Inventory inventory) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getType().name(),
                product.getPrice(),
                product.getStatus().name(),
                inventory.isAvailable(),
                inventory.getQuantity()
        );
    }
}
