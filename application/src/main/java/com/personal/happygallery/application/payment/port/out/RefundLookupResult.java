package com.personal.happygallery.application.payment.port.out;

/** PG 결제 조회로 확인한 환불 상태. */
public record RefundLookupResult(
        Status status,
        String paymentKey,
        long cancelAmount,
        String refundTransactionKey,
        String reason
) {

    public static RefundLookupResult refunded(
            String paymentKey, long cancelAmount, String refundTransactionKey) {
        return new RefundLookupResult(
                Status.REFUNDED, paymentKey, cancelAmount, refundTransactionKey, null);
    }

    public static RefundLookupResult notRefunded(String paymentKey, String reason) {
        return new RefundLookupResult(Status.NOT_REFUNDED, paymentKey, 0L, null, reason);
    }

    public static RefundLookupResult reviewRequired(String paymentKey, String reason) {
        return new RefundLookupResult(Status.REVIEW_REQUIRED, paymentKey, 0L, null, reason);
    }

    public static RefundLookupResult unavailable(String paymentKey, String reason) {
        return new RefundLookupResult(Status.UNAVAILABLE, paymentKey, 0L, null, reason);
    }

    public enum Status {
        REFUNDED,
        NOT_REFUNDED,
        REVIEW_REQUIRED,
        UNAVAILABLE
    }
}
