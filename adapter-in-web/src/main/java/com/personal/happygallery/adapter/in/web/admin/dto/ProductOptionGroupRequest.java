package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionGroupDefinition;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import com.personal.happygallery.domain.product.ProductOptionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductOptionGroupRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String key,
        @NotNull ProductOptionType type,
        @NotBlank @Size(max = ProductOptionPolicy.MAX_NAME_LENGTH) String name,
        boolean required,
        @PositiveOrZero int sortOrder,
        @Size(max = ProductOptionPolicy.MAX_PLACEHOLDER_LENGTH) String inputPlaceholder,
        @Min(1) @Max(ProductOptionPolicy.MAX_INPUT_LENGTH) Integer inputMaxLength,
        @PositiveOrZero @Max(PaymentAmountPolicy.MAX_AMOUNT) Long inputPriceAdjustment,
        @NotNull @Size(max = ProductOptionPolicy.MAX_COMBINATIONS)
        List<@NotNull @Valid ProductOptionValueRequest> values
) {
    public OptionGroupDefinition toCommand() {
        return new OptionGroupDefinition(
                key, type, name, required, sortOrder, inputPlaceholder,
                inputMaxLength, inputPriceAdjustment,
                values.stream().map(ProductOptionValueRequest::toCommand).toList());
    }
}
