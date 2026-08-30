package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.VariantDefinition;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductVariantRequest(
        @NotNull @Size(max = ProductOptionPolicy.MAX_SELECT_GROUPS)
        List<@NotNull @Valid ProductVariantSelectionRequest> selections,
        long priceAdjustment,
        @PositiveOrZero int quantity,
        boolean active
) {
    public VariantDefinition toCommand() {
        return new VariantDefinition(
                selections.stream().map(ProductVariantSelectionRequest::toCommand).toList(),
                priceAdjustment, quantity, active);
    }
}
