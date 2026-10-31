package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;

/** NHN Cloud 발송 응답 본문의 판정 결과. ACCEPTED일 때만 requestId·recipientSeq를 가진다. */
record NhnSendOutcome(
        NotificationSendResult result,
        String diagnosticCode,
        String requestId,
        Long recipientSeq
) {
    static NhnSendOutcome immediate(NotificationSendResult result, String diagnosticCode) {
        return new NhnSendOutcome(result, diagnosticCode, null, null);
    }

    static NhnSendOutcome accepted(String requestId, Long recipientSeq) {
        return new NhnSendOutcome(NotificationSendResult.ACCEPTED, "0", requestId, recipientSeq);
    }
}
