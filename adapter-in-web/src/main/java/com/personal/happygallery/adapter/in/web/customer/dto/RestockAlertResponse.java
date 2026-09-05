package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.product.RestockAlert;
import com.personal.happygallery.domain.product.RestockAlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record RestockAlertResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String optionLabel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestockAlertStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime notifiedAt) {
    public static RestockAlertResponse from(RestockAlert alert, String productName) {
        return new RestockAlertResponse(alert.getId(), alert.getProductId(), alert.getProductVariantId(),
                productName, alert.getOptionLabel(), alert.getStatus(), alert.getCreatedAt(), alert.getNotifiedAt());
    }
}
