package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = Product.MAX_NAME_LENGTH) String name,
        @Size(max = 50) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive @Max(Product.MAX_PRICE) long price,
        @Size(max = Product.MAX_DESCRIPTION_LENGTH) String description,
        @Size(max = Product.MAX_IMAGE_URL_LENGTH) String imageUrl,
        @Size(max = Product.MAX_SPECIFICATION_LENGTH) String specification,
        @Size(max = Product.MAX_CARE_INSTRUCTIONS_LENGTH) String careInstructions,
        @Min(Product.MIN_PRODUCTION_LEAD_DAYS)
        @Max(Product.MAX_PRODUCTION_LEAD_DAYS)
        Integer productionLeadDays
) {}
