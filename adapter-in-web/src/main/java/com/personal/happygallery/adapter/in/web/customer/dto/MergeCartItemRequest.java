package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MergeCartItemRequest(
        @NotNull @Positive Long productId,
        @Min(1) int qty) {}
