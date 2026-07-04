package com.personal.happygallery.application.payment.port.out;

/** PG 결제 승인(confirm) 결과. */
public record PaymentConfirmResult(boolean success, String paymentKey, String method, String approvedAt, String failReason) {

    public static PaymentConfirmResult success(String paymentKey, String method, String approvedAt) {
        return new PaymentConfirmResult(true, paymentKey, method, approvedAt, null);
    }

    public static PaymentConfirmResult failure(String failReason) {
        return new PaymentConfirmResult(false, null, null, null, failReason);
    }
}
