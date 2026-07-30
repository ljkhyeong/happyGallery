package com.personal.happygallery.adapter.in.web.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ConfirmPaymentRequest(
        /** 0원 결제(8회권 사용 예약 등)일 때는 null 허용. */
        String paymentKey,
        @NotBlank String orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero long amount
) {}
