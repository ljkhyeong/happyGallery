package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;

public record CartItemDetail(
        Long cartItemId,
        Long productId,
        String productName,
        ProductType productType,
        long price,
        String specification,
        String careInstructions,
        Integer productionLeadDays,
        int qty,
        ProductStatus productStatus,
        Integer inventoryQuantity
) {}
