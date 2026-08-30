package com.personal.happygallery.adapter.in.web.webhook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TossPaymentWebhookRequest(
        @NotBlank @Size(max = 50) String eventType,
        @Valid @NotNull PaymentData data
) {
    public record PaymentData(
            @NotBlank @Size(max = 64) String orderId
    ) {}
}
