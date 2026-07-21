package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.Product;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = Product.MAX_NAME_LENGTH) String name,
        @Size(max = 50) String category,
        @Positive @Max(Product.MAX_PRICE) long price,
        @Size(max = Product.MAX_DESCRIPTION_LENGTH) String description,
        @Size(max = Product.MAX_IMAGE_URL_LENGTH) String imageUrl
) {}
