package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.dto.SmartStoreExchangeRejectCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSmartStoreExchangeRequest(
        @NotBlank @Size(max = 500) String reason
) {
    public SmartStoreExchangeRejectCommand toCommand(String productOrderId) {
        return new SmartStoreExchangeRejectCommand(productOrderId, reason);
    }
}
