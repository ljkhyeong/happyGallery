package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.notification.dto.AlimtalkRequest;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** NHN Cloud Alimtalk v2.2 실제 발송 어댑터. */
public class NhnAlimtalkSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NhnAlimtalkSender.class);

    private final AlimtalkNotificationProperties properties;
    private final RestClient restClient;

    public NhnAlimtalkSender(AlimtalkNotificationProperties properties,
                             RestClient alimtalkRestClient) {
        this.properties = properties;
        this.restClient = alimtalkRestClient;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public boolean send(String idempotencyKey,
                        String phone,
                        String recipientName,
                        NotificationEventType eventType) {
        try {
            AlimtalkRequest request = new AlimtalkRequest(
                    properties.senderKey(),
                    KakaoTemplateCatalog.resolveTemplateCode(eventType),
                    List.of(new AlimtalkRequest.Recipient(
                            phone,
                            Map.of("name", recipientName))));
            AlimtalkResponse response = restClient.post()
                    .uri("/alimtalk/v2.2/appkeys/{appKey}/messages", properties.appKey())
                    .header("X-Secret-Key", properties.secretKey())
                    .header("X-NC-API-IDEMPOTENCY-KEY", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(AlimtalkResponse.class);

            if (response == null || !response.successful()) {
                log.warn("[ALIMTALK] 발송 거절 [event={} resultCode={}]",
                        eventType, response == null ? "NO_BODY" : response.resultCode());
                return false;
            }
            log.info("[ALIMTALK] 발송 성공 event={}", eventType);
            return true;
        } catch (RestClientResponseException exception) {
            log.warn("[ALIMTALK] HTTP {} event={}", exception.getStatusCode(), eventType);
            return false;
        } catch (Exception exception) {
            log.warn("[ALIMTALK] 발송 예외 [event={} type={}]",
                    eventType, exception.getClass().getSimpleName());
            return false;
        }
    }

    private record AlimtalkResponse(ResponseHeader header, Message message) {
        private boolean successful() {
            return header != null
                    && header.isSuccessful()
                    && header.resultCode() == 0
                    && message != null
                    && message.sendResults() != null
                    && message.sendResults().size() == 1
                    && message.sendResults().getFirst().resultCode() == 0;
        }

        private String resultCode() {
            if (header == null) {
                return "NO_HEADER";
            }
            if (!header.isSuccessful() || header.resultCode() != 0) {
                return String.valueOf(header.resultCode());
            }
            if (message == null || message.sendResults() == null || message.sendResults().isEmpty()) {
                return "NO_SEND_RESULT";
            }
            if (message.sendResults().size() != 1) {
                return "UNEXPECTED_SEND_RESULT_COUNT:" + message.sendResults().size();
            }
            return String.valueOf(message.sendResults().getFirst().resultCode());
        }
    }

    private record ResponseHeader(boolean isSuccessful, int resultCode) {}

    private record Message(List<SendResult> sendResults) {}

    private record SendResult(int resultCode) {}
}
