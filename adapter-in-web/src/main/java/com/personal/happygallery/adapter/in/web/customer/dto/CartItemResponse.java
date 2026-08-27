package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

public record CartItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long cartItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductType productType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long basePrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long variantPriceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long textOptionPriceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String specification,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String careInstructions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Integer productionLeadDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        java.util.List<ProductOptionSnapshotResponse> options,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int qty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long subtotal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available
) {}
