package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.product.ProductType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductRequest(
        @NotBlank String name,
        @NotNull ProductType type,
        @Positive long price,
        @Min(1) int quantity
) {}
