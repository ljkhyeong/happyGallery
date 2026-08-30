package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RequestSmartStoreSellerReturnRequest(
        @NotBlank String returnReason,
        @NotBlank String collectDeliveryMethod,
        String collectDeliveryCompany,
        String collectTrackingNumber,
        @Positive Integer returnQuantity
) {
    public SmartStoreChannelOrderUseCase.SellerReturnCommand toCommand(String productOrderId) {
        return new SmartStoreChannelOrderUseCase.SellerReturnCommand(
                productOrderId, returnReason, collectDeliveryMethod, collectDeliveryCompany,
                collectTrackingNumber, returnQuantity);
    }
}
