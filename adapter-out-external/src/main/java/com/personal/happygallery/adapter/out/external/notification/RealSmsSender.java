package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.adapter.out.external.notification.dto.SmsRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * SMS 실제 발송 어댑터 (NHN Cloud SMS).
 * prod 프로필에서 {@link NotificationResilienceConfig}가
 * {@link ResilientNotificationSender}로 감싸 등록한다.
 */
public class RealSmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(RealSmsSender.class);

    private final SmsNotificationProperties properties;
    private final RestClient restClient;
    private final SmsMessageCatalog messageCatalog;

    public RealSmsSender(SmsNotificationProperties properties,
                         RestClient smsRestClient,
                         SmsMessageCatalog messageCatalog) {
        this.properties = properties;
        this.restClient = smsRestClient;
        this.messageCatalog = messageCatalog;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        try {
            String message = messageCatalog.render(recipientName, eventType);
            var request = new SmsRequest(
                    message, properties.senderNumber(),
                    List.of(new SmsRequest.Recipient(phone)));

            var response = restClient.post()
                    .uri("/sms/v3.0/appKeys/{apiKey}/sender/sms", properties.apiKey())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("[SMS] HTTP {} phone={} event={}", resp.getStatusCode(), phone, eventType);
                    })
                    .toBodilessEntity();

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("[SMS] 발송 성공 phone={} event={}", phone, eventType);
            }
            return success;
        } catch (Exception e) {
            log.warn("[SMS] 발송 예외 phone={} event={}", phone, eventType, e);
            return false;
        }
    }
}
