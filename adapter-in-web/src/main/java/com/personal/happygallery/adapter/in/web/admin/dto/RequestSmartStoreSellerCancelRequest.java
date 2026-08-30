package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequestSmartStoreSellerCancelRequest(
        @NotBlank String reason,
        @Size(max = 500) String detailedReason,
        @Positive Integer quantity
) {
    public SmartStoreChannelOrderUseCase.SellerCancelCommand toCommand(String productOrderId) {
        return new SmartStoreChannelOrderUseCase.SellerCancelCommand(
                productOrderId, reason, detailedReason, quantity);
    }
}
