package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentReconciliationResultResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long attemptId,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"RECONCILIATION_REQUIRED", "CONFIRMED", "FAILED"})
        PaymentAttemptStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long domainId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message
) {

    public static PaymentReconciliationResultResponse from(
            PaymentReconciliationAdminUseCase.ReconciliationResult result) {
        return new PaymentReconciliationResultResponse(
                result.attemptId(),
                result.status(),
                result.domainId(),
                result.message());
    }
}
