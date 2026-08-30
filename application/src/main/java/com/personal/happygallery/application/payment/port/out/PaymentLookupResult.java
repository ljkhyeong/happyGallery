package com.personal.happygallery.application.payment.port.out;

public record PaymentLookupResult(
        Status status,
        String paymentKey,
        String orderId,
        long totalAmount,
        String method,
        String reason
) {

    public static PaymentLookupResult approved(
            String paymentKey, String orderId, long totalAmount, String method) {
        return new PaymentLookupResult(Status.APPROVED, paymentKey, orderId, totalAmount, method, null);
    }

    public static PaymentLookupResult notApproved(String orderId, String reason) {
        return new PaymentLookupResult(Status.NOT_APPROVED, null, orderId, 0L, null, reason);
    }

    public static PaymentLookupResult reviewRequired(String orderId, String reason) {
        return new PaymentLookupResult(Status.REVIEW_REQUIRED, null, orderId, 0L, null, reason);
    }

    public static PaymentLookupResult unavailable(String orderId, String reason) {
        return new PaymentLookupResult(Status.UNAVAILABLE, null, orderId, 0L, null, reason);
    }

    public enum Status {
        APPROVED,
        NOT_APPROVED,
        REVIEW_REQUIRED,
        UNAVAILABLE
    }
}
