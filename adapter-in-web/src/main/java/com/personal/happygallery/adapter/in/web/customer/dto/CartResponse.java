package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.cart.port.in.CartUseCase.CartView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CartResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CartItemResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String cartVersion
) {
    public static CartResponse from(CartView view) {
        List<CartItemResponse> items = view.items().stream()
                .map(i -> new CartItemResponse(
                        i.cartItemId(), i.productId(), i.productVariantId(),
                        i.productName(), i.productType(), i.basePrice(),
                        i.variantPriceAdjustment(), i.textOptionPriceAdjustment(), i.price(),
                        i.specification(), i.careInstructions(), i.productionLeadDays(),
                        i.options().stream().map(ProductOptionSnapshotResponse::from).toList(),
                        i.qty(), i.subtotal(), i.available(), i.availableQuantity()))
                .toList();
        return new CartResponse(items, view.totalAmount(), view.cartVersion());
    }
}
