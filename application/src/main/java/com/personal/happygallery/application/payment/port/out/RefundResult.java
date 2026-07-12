package com.personal.happygallery.application.payment.port.out;

/** PG 환불 호출 결과 (port 계약) */
public record RefundResult(Outcome outcome, String refundTransactionKey, String failReason) {

    public static RefundResult success(String refundTransactionKey) {
        return new RefundResult(Outcome.SUCCESS, refundTransactionKey, null);
    }

    /** PG가 명시적으로 거절해 같은 요청의 자동 재시도가 의미 없는 경우다. */
    public static RefundResult failure(String failReason) {
        return new RefundResult(Outcome.FINAL_FAILURE, null, failReason);
    }

    /** PG 호출이 실행되지 않았거나 명시적인 일시 실패로 안전하게 재시도할 수 있다. */
    public static RefundResult retryableFailure(String failReason) {
        return new RefundResult(Outcome.RETRYABLE_FAILURE, null, failReason);
    }

    /** 요청이 PG에 반영됐는지 알 수 없어 같은 멱등키로 결과를 확인해야 한다. */
    public static RefundResult reconciliationRequired(String failReason) {
        return new RefundResult(Outcome.RECONCILIATION_REQUIRED, null, failReason);
    }

    public boolean success() {
        return outcome == Outcome.SUCCESS;
    }

    public boolean retryable() {
        return outcome == Outcome.RETRYABLE_FAILURE;
    }

    public boolean reconciliationRequired() {
        return outcome == Outcome.RECONCILIATION_REQUIRED;
    }

    public enum Outcome {
        SUCCESS,
        FINAL_FAILURE,
        RETRYABLE_FAILURE,
        RECONCILIATION_REQUIRED
    }
}
