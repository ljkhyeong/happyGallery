package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.product.ProductType;

public record CartItemResponse(Long productId, String productName, ProductType productType, long price,
                               int qty, long subtotal, boolean available) {}
