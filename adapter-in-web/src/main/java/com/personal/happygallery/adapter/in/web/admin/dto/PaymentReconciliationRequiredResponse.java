package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PaymentReconciliationRequiredResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long attemptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentContext context,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = "RECONCILIATION_REQUIRED")
        PaymentAttemptStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
) {

    public static PaymentReconciliationRequiredResponse from(PaymentAttempt attempt) {
        LocalDateTime createdAt = attempt.getCreatedAt();
        return new PaymentReconciliationRequiredResponse(
                attempt.getId(),
                attempt.getContext(),
                attempt.getAmount(),
                attempt.getStatus(),
                attempt.getFailReason(),
                createdAt.atOffset(ZoneOffset.UTC));
    }
}
