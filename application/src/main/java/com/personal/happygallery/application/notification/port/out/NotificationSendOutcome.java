package com.personal.happygallery.application.notification.port.out;

public record NotificationSendOutcome(
        NotificationSendResult result,
        String providerRequestId,
        Long providerRecipientSeq
) {
    public NotificationSendOutcome {
        if (result == NotificationSendResult.ACCEPTED
                && (providerRequestId == null || providerRequestId.isBlank()
                    || providerRecipientSeq == null)) {
            throw new IllegalArgumentException("접수된 알림에는 제공자 요청 식별자가 필요합니다.");
        }
    }

    public static NotificationSendOutcome immediate(NotificationSendResult result) {
        return new NotificationSendOutcome(result, null, null);
    }

    public static NotificationSendOutcome accepted(String requestId, Long recipientSeq) {
        return new NotificationSendOutcome(
                NotificationSendResult.ACCEPTED, requestId, recipientSeq);
    }
}
