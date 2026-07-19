package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;

public record PaymentReconciliationResultResponse(
        Long attemptId,
        PaymentAttemptStatus status,
        Long domainId,
        String message
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
