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
                        i.productId(), i.productName(), i.productType(), i.price(),
                        i.specification(), i.careInstructions(), i.productionLeadDays(),
                        i.qty(), i.subtotal(), i.available()))
                .toList();
        return new CartResponse(items, view.totalAmount(), view.cartVersion());
    }
}
