package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.payment.PaymentContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PreparePaymentRequest(
        @NotNull PaymentContext context,
        @NotNull @Valid PaymentPayload payload
) {}
