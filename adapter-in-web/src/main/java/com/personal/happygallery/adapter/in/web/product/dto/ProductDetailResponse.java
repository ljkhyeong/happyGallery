package com.personal.happygallery.adapter.in.web.product.dto;

import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ProductType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String specification,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String careInstructions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Integer productionLeadDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean available,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0",
                description = "현재 재고 수량. 주문제작은 활성 옵션 조합의 합계")
        long stockQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProductOptionGroupResponse> optionGroups,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProductVariantResponse> variants
) {
    public static ProductDetailResponse from(ProductQueryUseCase.ProductView r) {
        Product product = r.product();
        return new ProductDetailResponse(
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
                product.getStatus() == ProductStatus.ACTIVE && r.available(),
                r.quantity(),
                r.options().groups().stream().map(ProductOptionGroupResponse::from).toList(),
                r.options().variants().stream().map(ProductVariantResponse::from).toList()
        );
    }
}
