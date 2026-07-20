package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.order.OrderAmountCalculator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MergeCartItemRequest(
        @NotNull @Positive Long productId,
        @Min(1) @Max(OrderAmountCalculator.MAX_ITEM_QUANTITY) int qty) {}
