package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.CurrentOrderStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreCurrentOrderStatusResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productOrderStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String placeOrderStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String claimType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String claimStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int remainQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime shippingDueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String expectedDeliveryMethod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String deliveryCompany,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        SmartStoreChannelOrderDetailResponse.ClaimDetail claimDetail
) {
    public static SmartStoreCurrentOrderStatusResponse from(CurrentOrderStatusResult result) {
        return new SmartStoreCurrentOrderStatusResponse(
                result.productOrderId(), result.productOrderStatus(), result.placeOrderStatus(),
                result.claimType(), result.claimStatus(), result.remainQuantity(),
                result.shippingDueDate(), result.expectedDeliveryMethod(), result.deliveryCompany(),
                result.trackingNumber(), result.claimDetail() == null
                        ? null : SmartStoreChannelOrderDetailResponse.ClaimDetail.from(result.claimDetail()));
    }
}
