package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderResult;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreChannelOrderResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long originProductNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long itemNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long productVariantId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productOption,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String claimType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String claimStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int initialQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int remainQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int inventoryAppliedQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        SmartStoreOrderAttentionReason attentionReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime paymentDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime lastChangedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pendingReturnQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String returnReviewVersion
) {
    public static SmartStoreChannelOrderResponse from(ChannelOrderResult result) {
        return new SmartStoreChannelOrderResponse(
                result.productOrderId(), result.orderId(), result.originProductNo(), result.itemNo(),
                result.productId(), result.productVariantId(), result.productName(),
                result.productOption(), result.productOrderStatus(), result.claimType(),
                result.claimStatus(), result.initialQuantity(), result.remainQuantity(),
                result.inventoryAppliedQuantity(), result.attentionReason(), result.paymentDate(),
                result.lastChangedAt(), result.pendingReturnQuantity(), result.returnReviewVersion());
    }
}
