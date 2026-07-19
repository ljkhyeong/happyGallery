package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
        @NotNull ProductStatus status
) {}
