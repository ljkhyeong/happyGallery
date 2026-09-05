package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockThresholdRequest(
        @NotNull @Positive Long productId,
        @Positive @Schema(nullable = true) Long productVariantId,
        @PositiveOrZero @Schema(nullable = true, description = "null이면 재고 부족 기준을 해제하고 품절만 표시") Integer minimumStock,
        @NotNull @PositiveOrZero Long version) {}
