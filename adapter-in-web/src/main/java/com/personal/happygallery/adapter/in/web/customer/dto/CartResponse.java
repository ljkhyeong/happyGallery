package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.cart.port.in.CartUseCase.CartView;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, long totalAmount) {
    public static CartResponse from(CartView view) {
        List<CartItemResponse> items = view.items().stream()
                .map(i -> new CartItemResponse(
                        i.productId(), i.productName(), i.price(),
                        i.qty(), i.subtotal(), i.available()))
                .toList();
        return new CartResponse(items, view.totalAmount());
    }
}
