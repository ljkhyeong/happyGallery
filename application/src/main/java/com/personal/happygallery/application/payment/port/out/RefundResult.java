package com.personal.happygallery.application.payment.port.out;

/** PG 환불 호출 결과 (port 계약) */
public record RefundResult(boolean success, String refundPgRef, String failReason) {

    public static RefundResult success(String refundPgRef) {
        return new RefundResult(true, refundPgRef, null);
    }

    public static RefundResult failure(String failReason) {
        return new RefundResult(false, null, failReason);
    }
}
