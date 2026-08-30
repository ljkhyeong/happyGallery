package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ExchangeDispatchCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DispatchSmartStoreExchangeRequest(
        @NotBlank @Pattern(regexp = "^[A-Z_]{1,40}$") String deliveryMethod,
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{1,40}$") String deliveryCompanyCode,
        @NotBlank @Size(max = 100) String trackingNumber
) {
    public ExchangeDispatchCommand toCommand(String productOrderId) {
        return new ExchangeDispatchCommand(
                productOrderId, deliveryMethod, deliveryCompanyCode, trackingNumber);
    }
}
