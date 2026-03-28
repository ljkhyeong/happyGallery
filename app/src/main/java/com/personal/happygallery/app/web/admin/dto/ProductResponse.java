package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.app.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;

public record ProductResponse(
        Long id,
        String name,
        String type,
        String category,
        long price,
        String status,
        boolean available,
        int quantity
) {
    public static ProductResponse from(ProductQueryUseCase.ProductWithInventory r) {
        return from(r.product(), r.inventory());
    }

    public static ProductResponse from(ProductAdminUseCase.RegisterResult r) {
        return from(r.product(), r.inventory());
    }

    private static ProductResponse from(Product product, Inventory inventory) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getType().name(),
                product.getCategory(),
                product.getPrice(),
                product.getStatus().name(),
                inventory.isAvailable(),
                inventory.getQuantity()
        );
    }
}
