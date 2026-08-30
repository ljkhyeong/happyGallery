package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.order.OrderAmountCalculator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record AddCartItemRequest(
        @NotNull Long productId,
        @Positive @Schema(nullable = true) Long productVariantId,
        @Size(max = 5) List<@NotNull @Valid ProductTextInputRequest> textInputs,
        @Min(1) @Max(OrderAmountCalculator.MAX_ITEM_QUANTITY)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        int qty) {
    public AddCartItemRequest {
        textInputs = textInputs == null ? List.of() : List.copyOf(textInputs);
    }

    public AddCartItemRequest(Long productId, int qty) {
        this(productId, null, List.of(), qty);
    }
}
