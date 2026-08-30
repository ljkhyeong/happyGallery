package com.personal.happygallery.adapter.in.web.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PassPaymentPolicyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int validityDays
) {}
