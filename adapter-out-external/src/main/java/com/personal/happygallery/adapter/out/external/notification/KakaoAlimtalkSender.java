package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.adapter.out.external.notification.dto.KakaoAlimtalkRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * 카카오 알림톡 실제 발송 어댑터.
 * prod 프로필에서 {@link NotificationResilienceConfig}가
 * {@link ResilientNotificationSender}로 감싸 등록한다.
 */
public class KakaoAlimtalkSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(KakaoAlimtalkSender.class);

    private final KakaoNotificationProperties properties;
    private final RestClient restClient;
    private final KakaoTemplateCatalog templateCatalog;

    public KakaoAlimtalkSender(KakaoNotificationProperties properties,
                               RestClient kakaoRestClient,
                               KakaoTemplateCatalog templateCatalog) {
        this.properties = properties;
        this.restClient = kakaoRestClient;
        this.templateCatalog = templateCatalog;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        try {
            String templateCode = templateCatalog.resolveTemplateCode(eventType);
            var request = new KakaoAlimtalkRequest(
                    properties.senderKey(), templateCode, phone,
                    Map.of("name", recipientName));

            var response = restClient.post()
                    .uri("/v1/api/talk/friends/message/default/send")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("[KAKAO] HTTP {} phone={} event={}", resp.getStatusCode(), phone, eventType);
                    })
                    .toBodilessEntity();

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("[KAKAO] 발송 성공 phone={} event={}", phone, eventType);
            }
            return success;
        } catch (Exception e) {
            log.warn("[KAKAO] 발송 예외 phone={} event={}", phone, eventType, e);
            return false;
        }
    }
}
