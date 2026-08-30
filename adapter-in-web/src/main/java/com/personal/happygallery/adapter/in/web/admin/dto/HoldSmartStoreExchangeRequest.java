package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HoldSmartStoreExchangeRequest(
        @NotBlank String holdbackClassType,
        @NotBlank @Size(max = 500) String detailedReason,
        @PositiveOrZero Long extraExchangeFeeAmount
) {
    public SmartStoreChannelOrderUseCase.ExchangeHoldCommand toCommand(String productOrderId) {
        return new SmartStoreChannelOrderUseCase.ExchangeHoldCommand(
                productOrderId, holdbackClassType, detailedReason, extraExchangeFeeAmount);
    }
}
