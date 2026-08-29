package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationEventType;

public interface TrackedNotificationSenderPort extends NotificationSenderPort {

    NotificationSendOutcome sendTracked(
            String idempotencyKey,
            String phone,
            String recipientName,
            NotificationEventType eventType);
}
