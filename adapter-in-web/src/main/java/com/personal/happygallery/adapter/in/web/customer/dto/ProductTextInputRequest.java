package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductTextInputRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String groupKey,
        @Size(max = ProductOptionPolicy.MAX_INPUT_LENGTH) String value
) {
    public TextInput toCommand() {
        return new TextInput(groupKey, value);
    }
}
