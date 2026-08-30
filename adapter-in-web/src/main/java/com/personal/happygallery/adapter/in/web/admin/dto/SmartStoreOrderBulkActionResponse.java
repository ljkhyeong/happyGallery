package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.BulkOperationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreOrderBulkActionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> successProductOrderIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Failure> failures
) {
    public static SmartStoreOrderBulkActionResponse from(BulkOperationResult result) {
        return new SmartStoreOrderBulkActionResponse(
                result.successProductOrderIds(),
                result.failures().stream()
                        .map(failure -> new Failure(
                                failure.productOrderId(), failure.code(), failure.message()))
                        .toList());
    }

    public record Failure(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String code,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message
    ) {}
}
