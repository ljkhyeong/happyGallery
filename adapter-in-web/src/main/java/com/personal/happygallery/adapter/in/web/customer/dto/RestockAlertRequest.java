package com.personal.happygallery.adapter.in.web.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestockAlertRequest(@NotNull @Positive Long productId,
                                  @Positive @Schema(nullable = true) Long productVariantId) {}
