package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreChannelOrderDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SmartStoreChannelOrderResponse order,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) DeliveryInfo deliveryInfo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String placeOrderStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime shippingDueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String expectedDeliveryMethod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String deliveryCompany,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long unitPrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long paymentAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long paymentCommission,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long saleCommission,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long channelCommission,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long expectedSettlementAmount
) {
    public static SmartStoreChannelOrderDetailResponse from(ChannelOrderDetailResult result) {
        var delivery = result.deliveryInfo();
        return new SmartStoreChannelOrderDetailResponse(
                SmartStoreChannelOrderResponse.from(result.order()),
                delivery == null ? null : new DeliveryInfo(
                        delivery.recipientName(), delivery.phone(), delivery.postalCode(),
                        delivery.addressLine1(), delivery.addressLine2(), delivery.shippingMemo()),
                result.placeOrderStatus(), result.shippingDueDate(), result.expectedDeliveryMethod(),
                result.deliveryCompany(), result.trackingNumber(), result.unitPrice(),
                result.paymentAmount(), result.paymentCommission(), result.saleCommission(),
                result.channelCommission(), result.expectedSettlementAmount());
    }

    public record DeliveryInfo(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String recipientName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String phone,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String postalCode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String addressLine1,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String addressLine2,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String shippingMemo
    ) {}
}
