package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProductRequest(
        @NotBlank @Size(max = Product.MAX_NAME_LENGTH) String name,
        @Size(max = 50) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive @Max(Product.MAX_PRICE) long price,
        @PositiveOrZero @Schema(nullable = true) Integer quantity,
        @Size(max = Product.MAX_DESCRIPTION_LENGTH) String description,
        @Size(max = Product.MAX_IMAGE_URL_LENGTH) String imageUrl,
        @Size(max = Product.MAX_SPECIFICATION_LENGTH) String specification,
        @Size(max = Product.MAX_CARE_INSTRUCTIONS_LENGTH) String careInstructions,
        @Min(Product.MIN_PRODUCTION_LEAD_DAYS)
        @Max(Product.MAX_PRODUCTION_LEAD_DAYS)
        Integer productionLeadDays,
        @Size(max = ProductOptionPolicy.MAX_SELECT_GROUPS + ProductOptionPolicy.MAX_TEXT_GROUPS)
        List<@NotNull @Valid ProductOptionGroupRequest> optionGroups,
        @Size(max = ProductOptionPolicy.MAX_COMBINATIONS)
        List<@NotNull @Valid ProductVariantRequest> variants
) {
    public UpdateProductRequest {
        optionGroups = optionGroups == null ? List.of() : List.copyOf(optionGroups);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }

    public ProductAdminUseCase.SaveProductCommand toCommand() {
        return new ProductAdminUseCase.SaveProductCommand(
                name, null, category, price, quantity, description, imageUrl,
                specification, careInstructions, productionLeadDays,
                optionGroups.stream().map(ProductOptionGroupRequest::toCommand).toList(),
                variants.stream().map(ProductVariantRequest::toCommand).toList());
    }
}
