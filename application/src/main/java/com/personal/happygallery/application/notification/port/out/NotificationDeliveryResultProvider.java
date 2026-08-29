package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationChannel;

public interface NotificationDeliveryResultProvider {

    NotificationChannel channel();

    NotificationDeliveryResult findResult(String requestId, Long recipientSeq);
}
