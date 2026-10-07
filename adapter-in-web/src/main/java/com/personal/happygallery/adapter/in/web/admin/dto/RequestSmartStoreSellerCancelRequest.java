package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.dto.SmartStoreSellerCancelCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequestSmartStoreSellerCancelRequest(
        @NotBlank String reason,
        @Size(max = 500) String detailedReason,
        @Positive Integer quantity
) {
    public SmartStoreSellerCancelCommand toCommand(String productOrderId) {
        return new SmartStoreSellerCancelCommand(
                productOrderId, reason, detailedReason, quantity);
    }
}
