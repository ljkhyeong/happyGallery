package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import org.springframework.web.client.RestClientResponseException;

final class NhnNotificationFailureClassifier {

    private NhnNotificationFailureClassifier() {}

    static NotificationSendResult classify(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 408 || status == 425 || status == 429 || status >= 500) {
            return NotificationSendResult.TRANSIENT_FAILURE;
        }
        return NotificationSendResult.PERMANENT_FAILURE;
    }
}
