package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase.CustomerPaymentStatus;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.RefundStatus;
import org.springframework.stereotype.Component;

@Component
class CustomerPaymentStatusResolver {

    private final RefundPort refundPort;

    CustomerPaymentStatusResolver(RefundPort refundPort) {
        this.refundPort = refundPort;
    }

    CustomerPaymentStatus resolve(PaymentAttempt attempt) {
        return switch (attempt.getStatus()) {
            case PENDING -> CustomerPaymentStatus.READY;
            case PROCESSING, APPROVED -> CustomerPaymentStatus.CONFIRMING;
            case RETRYABLE -> CustomerPaymentStatus.RETRYABLE;
            case CONFIRMED -> CustomerPaymentStatus.COMPLETED;
            case FAILED -> CustomerPaymentStatus.FAILED;
            case RECONCILIATION_REQUIRED -> CustomerPaymentStatus.REVIEW_REQUIRED;
            case COMPENSATION_REQUESTED, COMPENSATED, COMPENSATION_FAILED ->
                    compensationStatus(attempt);
            case CANCELED -> CustomerPaymentStatus.EXPIRED;
        };
    }

    private CustomerPaymentStatus compensationStatus(PaymentAttempt attempt) {
        return refundPort.findByPaymentAttemptId(attempt.getId())
                .map(refund -> refundStatus(refund.getStatus()))
                .orElse(CustomerPaymentStatus.SUPPORT_REQUIRED);
    }

    private CustomerPaymentStatus refundStatus(RefundStatus status) {
        return switch (status) {
            case REQUESTED, PROCESSING, RETRYABLE -> CustomerPaymentStatus.REFUNDING;
            case SUCCEEDED -> CustomerPaymentStatus.REFUNDED;
            case RECONCILIATION_REQUIRED, FAILED -> CustomerPaymentStatus.SUPPORT_REQUIRED;
        };
    }
}
