package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.payment.PaymentContext;

public interface PaymentStatusQueryUseCase {

    PaymentStatusResult getStatus(String orderId, AuthContext auth, String statusToken);

    enum CustomerPaymentStatus {
        READY,
        CONFIRMING,
        RETRYABLE,
        COMPLETED,
        FAILED,
        REVIEW_REQUIRED,
        REFUNDING,
        REFUNDED,
        SUPPORT_REQUIRED,
        EXPIRED
    }

    record PaymentStatusResult(
            PaymentContext context,
            long amount,
            CustomerPaymentStatus status,
            Long domainId,
            String accessToken,
            boolean accessRecoveryRequired,
            String receiptUrl
    ) {
        public PaymentStatusResult(
                PaymentContext context,
                long amount,
                CustomerPaymentStatus status,
                Long domainId,
                String accessToken,
                boolean accessRecoveryRequired) {
            this(context, amount, status, domainId, accessToken, accessRecoveryRequired, null);
        }
    }
}
