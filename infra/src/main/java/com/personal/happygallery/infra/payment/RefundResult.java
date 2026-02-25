package com.personal.happygallery.infra.payment;

/** PG 환불 호출 결과 */
public record RefundResult(boolean success, String pgRef, String failReason) {

    public static RefundResult success(String pgRef) {
        return new RefundResult(true, pgRef, null);
    }

    public static RefundResult failure(String failReason) {
        return new RefundResult(false, null, failReason);
    }
}
