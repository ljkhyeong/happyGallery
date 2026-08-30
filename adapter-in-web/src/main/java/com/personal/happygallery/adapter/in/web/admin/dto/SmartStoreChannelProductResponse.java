package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ChannelOptionResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ChannelProductResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreChannelProductResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long originProductNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long salePrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Option> options
) {
    public static SmartStoreChannelProductResponse from(ChannelProductResult result) {
        return new SmartStoreChannelProductResponse(
                result.originProductNo(), result.salePrice(), result.status(),
                result.options().stream().map(Option::from).toList());
    }

    public record Option(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long optionId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int stockQuantity,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long price,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean usable
    ) {
        private static Option from(ChannelOptionResult result) {
            return new Option(
                    result.optionId(), result.name(), result.stockQuantity(),
                    result.price(), result.usable());
        }
    }
}
