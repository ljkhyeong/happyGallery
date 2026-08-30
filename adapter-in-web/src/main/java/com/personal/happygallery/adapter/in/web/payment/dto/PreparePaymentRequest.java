package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.domain.payment.PaymentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PreparePaymentRequest(
        @NotNull PaymentContext context,
        @NotNull
        @Valid
        @Schema(
                oneOf = {
                        OrderPaymentPayloadRequest.class,
                        BookingPaymentPayloadRequest.class,
                        PassPaymentPayloadRequest.class
                })
        PaymentPayloadRequest payload
) {}
