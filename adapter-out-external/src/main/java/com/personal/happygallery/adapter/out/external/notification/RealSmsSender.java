package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.springframework.web.client.RestClient;

/**
 * SMS 실제 발송 어댑터 (NHN Cloud SMS).
 * prod 프로필에서 {@link NotificationResilienceConfig}가
 * {@link ResilientNotificationSender}로 감싸 등록한다.
 */
public class RealSmsSender implements NotificationSender {

    private final NhnSmsClient smsClient;

    public RealSmsSender(SmsNotificationProperties properties,
                         RestClient smsRestClient) {
        this.smsClient = new NhnSmsClient(properties, smsRestClient);
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationSendResult send(String idempotencyKey,
                                       String phone,
                                       String recipientName,
                                       NotificationEventType eventType) {
        return smsClient.send(
                phone,
                SmsMessageCatalog.render(recipientName, eventType),
                eventType.name());
    }
}
