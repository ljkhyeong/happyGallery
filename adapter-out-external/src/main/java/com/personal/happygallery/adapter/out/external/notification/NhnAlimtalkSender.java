package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.notification.dto.AlimtalkRequest;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

/** NHN Cloud Alimtalk v2.2 실제 발송 어댑터. */
public class NhnAlimtalkSender implements NotificationSenderPort {

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
    public NotificationSendResult send(String idempotencyKey,
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

            ResponseOutcome outcome = response == null
                    ? new ResponseOutcome(NotificationSendResult.DELIVERY_UNKNOWN, "NO_BODY")
                    : response.outcome();
            if (outcome.result() != NotificationSendResult.SUCCESS) {
                log.warn("[ALIMTALK] 발송 거절 [event={} resultCode={}]",
                        eventType, outcome.diagnosticCode());
                return outcome.result();
            }
            log.info("[ALIMTALK] 발송 성공 event={}", eventType);
            return NotificationSendResult.SUCCESS;
        } catch (RestClientResponseException exception) {
            log.warn("[ALIMTALK] HTTP {} event={}", exception.getStatusCode(), eventType);
            return NhnNotificationFailureClassifier.classify(exception);
        } catch (ResourceAccessException exception) {
            log.warn("[ALIMTALK] 네트워크 예외 [event={} type={}]",
                    eventType, exception.getClass().getSimpleName());
            return NhnNotificationFailureClassifier.classify(exception);
        } catch (Exception exception) {
            log.warn("[ALIMTALK] 발송 예외 [event={} type={}]",
                    eventType, exception.getClass().getSimpleName());
            return NotificationSendResult.DELIVERY_UNKNOWN;
        }
    }

    private record AlimtalkResponse(ResponseHeader header, Message message) {
        private ResponseOutcome outcome() {
            if (header == null) {
                return new ResponseOutcome(NotificationSendResult.DELIVERY_UNKNOWN, "NO_HEADER");
            }
            if (!header.isSuccessful() || header.resultCode() != 0) {
                return new ResponseOutcome(
                        NotificationSendResult.PERMANENT_FAILURE,
                        String.valueOf(header.resultCode()));
            }
            List<SendResult> sendResults = message == null ? null : message.sendResults();
            if (sendResults == null || sendResults.isEmpty()) {
                return new ResponseOutcome(NotificationSendResult.DELIVERY_UNKNOWN, "NO_SEND_RESULT");
            }
            if (sendResults.size() != 1) {
                return new ResponseOutcome(
                        NotificationSendResult.DELIVERY_UNKNOWN,
                        "UNEXPECTED_SEND_RESULT_COUNT:" + sendResults.size());
            }
            int resultCode = sendResults.getFirst().resultCode();
            NotificationSendResult result = resultCode == 0
                    ? NotificationSendResult.SUCCESS
                    : NotificationSendResult.PERMANENT_FAILURE;
            return new ResponseOutcome(result, String.valueOf(resultCode));
        }
    }

    private record ResponseOutcome(NotificationSendResult result, String diagnosticCode) {}

    private record ResponseHeader(boolean isSuccessful, int resultCode) {}

    private record Message(List<SendResult> sendResults) {}

    private record SendResult(int resultCode) {}
}
