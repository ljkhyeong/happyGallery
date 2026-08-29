package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ProductOptionPreview;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ProductPreviewResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreProductPreviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long productVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long originProductNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long localSalePrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long channelSalePrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String localStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String channelStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean different,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<OptionPreview> options
) {
    public static SmartStoreProductPreviewResponse from(ProductPreviewResult result) {
        return new SmartStoreProductPreviewResponse(
                result.productId(), result.productVersion(), result.originProductNo(),
                result.localSalePrice(), result.channelSalePrice(), result.localStatus(),
                result.channelStatus(), result.different(), result.options().stream()
                        .map(OptionPreview::from)
                        .toList());
    }

    public record OptionPreview(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long productVariantId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long optionId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long localPrice,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long channelPrice,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean localUsable,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Boolean channelUsable,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean different
    ) {
        private static OptionPreview from(ProductOptionPreview result) {
            return new OptionPreview(
                    result.productVariantId(), result.optionId(), result.localPrice(),
                    result.channelPrice(), result.localUsable(), result.channelUsable(),
                    result.different());
        }
    }
}
