package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSmartStoreExchangeRequest(
        @NotBlank @Size(max = 500) String reason
) {
    public SmartStoreChannelOrderUseCase.ExchangeRejectCommand toCommand(String productOrderId) {
        return new SmartStoreChannelOrderUseCase.ExchangeRejectCommand(productOrderId, reason);
    }
}
