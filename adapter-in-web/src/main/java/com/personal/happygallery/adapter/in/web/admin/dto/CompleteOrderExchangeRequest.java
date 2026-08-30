package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.domain.order.OrderClaim;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteOrderExchangeRequest(
        @NotBlank @Size(min = 1, max = OrderClaim.MAX_DELIVERY_VALUE_LENGTH) String carrier,
        @NotBlank @Size(min = 1, max = OrderClaim.MAX_DELIVERY_VALUE_LENGTH) String trackingNumber,
        @Size(max = OrderClaim.MAX_ADMIN_NOTE_LENGTH) String note
) {
    public AdminOrderClaimUseCase.CompleteExchangeCommand toCommand() {
        return new AdminOrderClaimUseCase.CompleteExchangeCommand(carrier, trackingNumber, note);
    }
}
