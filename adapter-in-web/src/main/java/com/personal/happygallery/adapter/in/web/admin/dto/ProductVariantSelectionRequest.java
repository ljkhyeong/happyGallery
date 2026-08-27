package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SelectionDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductVariantSelectionRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String groupKey,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String valueKey
) {
    public SelectionDefinition toCommand() {
        return new SelectionDefinition(groupKey, valueKey);
    }
}
