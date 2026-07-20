package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank String name,
        @NotNull ProductType type,
        @Size(max = 50) String category,
        @Positive @Max(Product.MAX_PRICE) long price,
        @Min(1) int quantity
) {}
