package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.dashboard.dto.TopProduct;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

public record TopProductResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = ProductType.class)
        String productType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalRevenue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalQuantity
) {

    public static TopProductResponse from(TopProduct product) {
        return new TopProductResponse(
                product.productId(),
                product.productName(),
                product.productType(),
                product.totalRevenue(),
                product.totalQuantity()
        );
    }
}
