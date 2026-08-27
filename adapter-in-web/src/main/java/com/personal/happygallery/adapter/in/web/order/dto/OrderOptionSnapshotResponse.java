package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.domain.order.OrderOptionSnapshot;
import com.personal.happygallery.domain.product.ProductOptionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderOptionSnapshotResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ProductOptionType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String groupName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String value,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long priceAdjustment
) {
    public static OrderOptionSnapshotResponse from(OrderOptionSnapshot option) {
        return new OrderOptionSnapshotResponse(
                option.getType(), option.getGroupName(),
                option.getValue(), option.getPriceAdjustment());
    }
}
