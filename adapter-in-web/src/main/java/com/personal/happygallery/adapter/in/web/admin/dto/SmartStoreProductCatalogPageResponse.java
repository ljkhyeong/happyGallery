package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.CatalogPageResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.CatalogProductResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreProductCatalogPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Product> products,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages
) {
    public static SmartStoreProductCatalogPageResponse from(CatalogPageResult result) {
        return new SmartStoreProductCatalogPageResponse(
                result.products().stream().map(Product::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public record Product(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long originProductNo,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long salePrice,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Integer stockQuantity,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String imageUrl
    ) {
        private static Product from(CatalogProductResult result) {
            return new Product(
                    result.originProductNo(), result.name(), result.status(), result.salePrice(),
                    result.stockQuantity(), result.imageUrl());
        }
    }
}
