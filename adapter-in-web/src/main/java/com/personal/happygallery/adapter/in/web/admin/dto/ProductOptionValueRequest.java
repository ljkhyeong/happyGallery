package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionValueDefinition;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductOptionValueRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String key,
        @NotBlank @Size(max = ProductOptionPolicy.MAX_NAME_LENGTH) String name,
        @PositiveOrZero int sortOrder
) {
    public OptionValueDefinition toCommand() {
        return new OptionValueDefinition(key, name, sortOrder);
    }
}
