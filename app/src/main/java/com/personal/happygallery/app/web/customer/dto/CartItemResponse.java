package com.personal.happygallery.app.web.customer.dto;

public record CartItemResponse(Long productId, String productName, long price,
                               int qty, long subtotal, boolean available) {}
