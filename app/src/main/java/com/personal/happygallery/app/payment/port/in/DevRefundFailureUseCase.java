package com.personal.happygallery.app.payment.port.in;

/** local/E2E 전용 — 다음 환불 호출을 강제 실패시킨다. */
public interface DevRefundFailureUseCase {

    void armNextFailure(String reason);

    void clear();
}
