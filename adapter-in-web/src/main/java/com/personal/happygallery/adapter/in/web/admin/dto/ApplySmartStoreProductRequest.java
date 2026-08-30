package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ApplySmartStoreProductRequest(
        @NotNull @PositiveOrZero Long productVersion
) {}
