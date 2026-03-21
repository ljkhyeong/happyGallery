package com.personal.happygallery.app.payment.port.out;

/**
 * 환불 실패 시뮬레이션 포트 (local/E2E 전용).
 */
public interface RefundFailureScriptPort {

    void armNextFailure(String reason);

    void clear();
}
