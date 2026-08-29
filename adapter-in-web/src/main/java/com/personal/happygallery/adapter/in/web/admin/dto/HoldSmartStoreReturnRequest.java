package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record HoldSmartStoreReturnRequest(
        @NotBlank String holdbackClassType,
        @NotBlank String detailedReason,
        @PositiveOrZero Long extraReturnFeeAmount
) {
    public SmartStoreChannelOrderUseCase.ReturnHoldCommand toCommand(String productOrderId) {
        return new SmartStoreChannelOrderUseCase.ReturnHoldCommand(
                productOrderId, holdbackClassType, detailedReason, extraReturnFeeAmount);
    }
}
