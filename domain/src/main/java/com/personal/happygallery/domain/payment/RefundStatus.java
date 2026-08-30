package com.personal.happygallery.domain.payment;

public enum RefundStatus {
    REQUESTED,
    PROCESSING,
    RETRYABLE,
    RECONCILIATION_REQUIRED,
    SUCCEEDED,
    FAILED
}
