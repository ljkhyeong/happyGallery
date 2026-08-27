package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.product.ProductOptions.OptionSnapshot;
import com.personal.happygallery.domain.product.ProductOptionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductOptionSnapshotResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductOptionType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String groupName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String value,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long priceAdjustment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder
) {
    public static ProductOptionSnapshotResponse from(OptionSnapshot snapshot) {
        return new ProductOptionSnapshotResponse(
                snapshot.type(), snapshot.groupName(), snapshot.value(),
                snapshot.priceAdjustment(), snapshot.sortOrder());
    }
}
