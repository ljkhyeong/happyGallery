package com.personal.happygallery.application.payment.port.out;

/** PG 결제 승인(confirm) 결과. */
public record PaymentConfirmResult(boolean success, String paymentKey, String method, String approvedAt,
                                   String failReason, FailureType failureType) {

    public static PaymentConfirmResult success(String paymentKey, String method, String approvedAt) {
        return new PaymentConfirmResult(true, paymentKey, method, approvedAt, null, FailureType.NONE);
    }

    public static PaymentConfirmResult failure(String failReason) {
        return new PaymentConfirmResult(false, null, null, null, failReason, FailureType.FINAL);
    }

    public static PaymentConfirmResult retryableFailure(String failReason) {
        return new PaymentConfirmResult(false, null, null, null, failReason, FailureType.RETRYABLE);
    }

    public static PaymentConfirmResult reconciliationRequired(String failReason) {
        return new PaymentConfirmResult(
                false, null, null, null, failReason, FailureType.RECONCILIATION_REQUIRED);
    }

    public boolean retryable() {
        return failureType == FailureType.RETRYABLE;
    }

    public boolean reconciliationRequired() {
        return failureType == FailureType.RECONCILIATION_REQUIRED;
    }

    public enum FailureType {
        NONE,
        FINAL,
        RETRYABLE,
        RECONCILIATION_REQUIRED
    }
}
