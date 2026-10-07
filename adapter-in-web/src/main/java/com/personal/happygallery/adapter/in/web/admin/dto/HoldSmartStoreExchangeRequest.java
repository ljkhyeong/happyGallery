package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.dto.SmartStoreExchangeHoldCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HoldSmartStoreExchangeRequest(
        @NotBlank String holdbackClassType,
        @NotBlank @Size(max = 500) String detailedReason,
        @PositiveOrZero Long extraExchangeFeeAmount
) {
    public SmartStoreExchangeHoldCommand toCommand(String productOrderId) {
        return new SmartStoreExchangeHoldCommand(
                productOrderId, holdbackClassType, detailedReason, extraExchangeFeeAmount);
    }
}
