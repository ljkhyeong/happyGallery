package com.personal.happygallery.application.product;

import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.StockThresholdPolicy;

public record StockLevel(Long productId, Long productVariantId, String productName, ProductType type,
                         int quantity, Integer minimumStock, long version, boolean active) {
    public boolean lowStock() {
        return StockThresholdPolicy.isLow(active, quantity, minimumStock);
    }
}
