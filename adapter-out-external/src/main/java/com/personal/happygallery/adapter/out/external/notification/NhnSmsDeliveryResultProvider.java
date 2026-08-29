package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResult;
import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResultProvider;
import com.personal.happygallery.domain.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class NhnSmsDeliveryResultProvider implements NotificationDeliveryResultProvider {

    private static final Logger log = LoggerFactory.getLogger(NhnSmsDeliveryResultProvider.class);

    private final SmsNotificationProperties properties;
    private final RestClient restClient;

    NhnSmsDeliveryResultProvider(SmsNotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationDeliveryResult findResult(String requestId, Long recipientSeq) {
        try {
            SmsResultResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sms/v3.0/appKeys/{apiKey}/sender/sms/{requestId}")
                            .queryParam("recipientSeq", recipientSeq)
                            .build(properties.apiKey(), requestId))
                    .retrieve()
                    .body(SmsResultResponse.class);
            return response == null
                    ? NotificationDeliveryResult.unavailable("NO_BODY")
                    : response.result();
        } catch (RestClientException exception) {
            log.warn("[SMS] 최종 결과 조회 실패 [requestId={} type={}]",
                    requestId, exception.getClass().getSimpleName());
            return NotificationDeliveryResult.unavailable(exception.getClass().getSimpleName());
        }
    }

    private record SmsResultResponse(ResponseHeader header, ResponseBody body) {
        private NotificationDeliveryResult result() {
            if (header == null || !header.isSuccessful() || header.resultCode() != 0) {
                return NotificationDeliveryResult.unavailable("INVALID_RESPONSE_HEADER");
            }
            if (body == null || body.data() == null || body.data().msgStatus() == null) {
                return NotificationDeliveryResult.unavailable("NO_MESSAGE_STATUS");
            }
            MessageResult data = body.data();
            return switch (data.msgStatus()) {
                case 3 -> data.resultCode() != null && data.resultCode() == 1000
                        ? NotificationDeliveryResult.delivered()
                        : NotificationDeliveryResult.failed(data.reason());
                case 0, 4, 5, 6 -> NotificationDeliveryResult.failed(data.reason());
                default -> NotificationDeliveryResult.pending();
            };
        }
    }

    private record ResponseHeader(boolean isSuccessful, int resultCode) {}

    private record ResponseBody(MessageResult data) {}

    private record MessageResult(Integer msgStatus, Integer resultCode, String resultMessage) {
        private String reason() {
            return resultCode == null ? resultMessage : String.valueOf(resultCode);
        }
    }
}
