package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.InspectionPageResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.InspectionProductResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreInspectionPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<InspectionProduct> products,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages
) {
    public static SmartStoreInspectionPageResponse from(InspectionPageResult result) {
        return new SmartStoreInspectionPageResponse(
                result.products().stream().map(InspectionProduct::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public record InspectionProduct(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long channelProductNo,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String action,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean restorationRequestAvailable
    ) {
        private static InspectionProduct from(InspectionProductResult result) {
            return new InspectionProduct(
                    result.channelProductNo(), result.reason(), result.action(),
                    result.restorationRequestAvailable());
        }
    }
}
