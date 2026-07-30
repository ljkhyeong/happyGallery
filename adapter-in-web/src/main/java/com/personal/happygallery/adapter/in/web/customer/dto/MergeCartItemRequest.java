package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.order.OrderAmountCalculator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MergeCartItemRequest(
        @NotNull @Positive Long productId,
        @Min(1) @Max(OrderAmountCalculator.MAX_ITEM_QUANTITY)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        int qty) {}
