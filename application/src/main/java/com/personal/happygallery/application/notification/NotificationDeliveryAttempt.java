package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendOutcome;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;

record NotificationDeliveryAttempt(
        NotificationSendResult result,
        NotificationChannel channel,
        String providerRequestId,
        Long providerRecipientSeq
) {
    static NotificationDeliveryAttempt from(
            NotificationChannel channel, NotificationSendOutcome outcome) {
        return new NotificationDeliveryAttempt(
                outcome.result(),
                channel,
                outcome.providerRequestId(),
                outcome.providerRecipientSeq());
    }

    static NotificationDeliveryAttempt immediate(NotificationSendResult result) {
        return new NotificationDeliveryAttempt(result, null, null, null);
    }
}
