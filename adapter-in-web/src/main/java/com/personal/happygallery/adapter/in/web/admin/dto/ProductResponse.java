package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.adapter.in.web.product.dto.ProductOptionGroupResponse;
import com.personal.happygallery.adapter.in.web.product.dto.ProductVariantResponse;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String specification,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String careInstructions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Integer productionLeadDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProductOptionGroupResponse> optionGroups,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProductVariantResponse> variants
) {
    public static ProductResponse from(ProductQueryUseCase.ProductView result) {
        return from(result.product(), result.quantity(), result.available(), result.options());
    }

    public static ProductResponse from(ProductAdminUseCase.ProductResult result) {
        return from(result.product(), result.quantity(), result.available(), result.options());
    }

    private static ProductResponse from(Product product, long quantity, boolean stockAvailable,
                                        com.personal.happygallery.application.product.ProductOptions options) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getType(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays(),
                product.getStatus(),
                product.getStatus() == ProductStatus.ACTIVE && stockAvailable,
                quantity,
                options.groups().stream().map(ProductOptionGroupResponse::from).toList(),
                options.variants().stream().map(ProductVariantResponse::from).toList()
        );
    }
}
