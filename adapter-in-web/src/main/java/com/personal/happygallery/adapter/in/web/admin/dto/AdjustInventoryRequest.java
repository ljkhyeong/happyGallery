package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdjustInventoryRequest(
        @NotNull InventoryAdjustmentType type,
        @Positive int quantity,
        @NotBlank @Size(max = 500) String reason
) {}
