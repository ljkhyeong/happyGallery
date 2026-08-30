package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.VariantDefinition;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductVariantRequest(
        @NotNull @Size(max = ProductOptionPolicy.MAX_SELECT_GROUPS)
        List<@NotNull @Valid ProductVariantSelectionRequest> selections,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long priceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "신규 조합의 최초 재고. 이미 등록된 조합은 요청값과 관계없이 현재 재고를 유지한다.")
        @PositiveOrZero int quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active
) {
    public VariantDefinition toCommand() {
        return new VariantDefinition(
                selections.stream().map(ProductVariantSelectionRequest::toCommand).toList(),
                priceAdjustment, quantity, active);
    }
}
