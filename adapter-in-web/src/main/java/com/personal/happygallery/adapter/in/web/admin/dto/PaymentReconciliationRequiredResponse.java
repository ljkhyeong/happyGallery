package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PaymentReconciliationRequiredResponse(
        Long attemptId,
        PaymentContext context,
        long amount,
        PaymentAttemptStatus status,
        String reason,
        OffsetDateTime createdAt
) {

    public static PaymentReconciliationRequiredResponse from(PaymentAttempt attempt) {
        LocalDateTime createdAt = attempt.getCreatedAt();
        return new PaymentReconciliationRequiredResponse(
                attempt.getId(),
                attempt.getContext(),
                attempt.getAmount(),
                attempt.getStatus(),
                attempt.getFailReason(),
                createdAt == null ? null : createdAt.atOffset(ZoneOffset.UTC));
    }
}
