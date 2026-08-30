package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResult;
import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResultProvider;
import com.personal.happygallery.domain.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class NhnAlimtalkDeliveryResultProvider implements NotificationDeliveryResultProvider {

    private static final Logger log = LoggerFactory.getLogger(NhnAlimtalkDeliveryResultProvider.class);

    private final AlimtalkNotificationProperties properties;
    private final RestClient restClient;

    NhnAlimtalkDeliveryResultProvider(
            AlimtalkNotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public NotificationDeliveryResult findResult(String requestId, Long recipientSeq) {
        try {
            AlimtalkResultResponse response = restClient.get()
                    .uri(
                            "/alimtalk/v2.2/appkeys/{appKey}/messages/{requestId}/{recipientSeq}",
                            properties.appKey(),
                            requestId,
                            recipientSeq)
                    .header("X-Secret-Key", properties.secretKey())
                    .retrieve()
                    .body(AlimtalkResultResponse.class);
            return response == null
                    ? NotificationDeliveryResult.unavailable("NO_BODY")
                    : response.result();
        } catch (RestClientException exception) {
            log.warn("[ALIMTALK] 최종 결과 조회 실패 [requestId={} type={}]",
                    requestId, exception.getClass().getSimpleName());
            return NotificationDeliveryResult.unavailable(exception.getClass().getSimpleName());
        }
    }

    private record AlimtalkResultResponse(ResponseHeader header, Message message) {
        private NotificationDeliveryResult result() {
            if (header == null || !header.isSuccessful() || header.resultCode() != 0) {
                return NotificationDeliveryResult.unavailable("INVALID_RESPONSE_HEADER");
            }
            if (message == null || message.messageStatus() == null) {
                return NotificationDeliveryResult.unavailable("NO_MESSAGE_STATUS");
            }
            return switch (message.messageStatus()) {
                case "COMPLETED" -> NotificationDeliveryResult.delivered();
                case "FAILED", "CANCEL" -> NotificationDeliveryResult.failed(
                        message.resultCode() == null ? message.messageStatus() : message.resultCode());
                default -> NotificationDeliveryResult.pending();
            };
        }
    }

    private record ResponseHeader(boolean isSuccessful, int resultCode) {}

    private record Message(String messageStatus, String resultCode) {}
}
