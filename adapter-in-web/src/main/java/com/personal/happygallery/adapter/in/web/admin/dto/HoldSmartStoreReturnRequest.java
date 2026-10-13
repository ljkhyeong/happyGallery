package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.dto.SmartStoreReturnHoldCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record HoldSmartStoreReturnRequest(
        @NotBlank String holdbackClassType,
        @NotBlank String detailedReason,
        @PositiveOrZero Long extraReturnFeeAmount
) {
    public SmartStoreReturnHoldCommand toCommand(String productOrderId) {
        return new SmartStoreReturnHoldCommand(
                productOrderId, holdbackClassType, detailedReason, extraReturnFeeAmount);
    }
}
