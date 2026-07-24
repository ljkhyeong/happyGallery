package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.notification.dto.SmsRequest;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

/** NHN Cloud SMS 요청 형식과 성공 판정을 한 곳에서 관리한다. */
final class NhnSmsClient {

    private static final Logger log = LoggerFactory.getLogger(NhnSmsClient.class);

    private final SmsNotificationProperties properties;
    private final RestClient restClient;

    NhnSmsClient(SmsNotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    NotificationSendResult send(String idempotencyKey, String phone, String message, String purpose) {
        return send(idempotencyKey, phone, message, purpose, "/sender/sms");
    }

    NotificationSendResult sendVerification(String phone, String message) {
        return send(null, phone, message, "PHONE_VERIFICATION", "/sender/auth/sms");
    }

    private NotificationSendResult send(String idempotencyKey,
                                        String phone,
                                        String message,
                                        String purpose,
                                        String senderPath) {
        try {
            SmsRequest request = new SmsRequest(
                    message,
                    properties.senderNumber(),
                    correlationId(idempotencyKey),
                    List.of(new SmsRequest.Recipient(phone)));
            SmsResponse response = restClient.post()
                    .uri("/sms/v3.0/appKeys/{apiKey}" + senderPath, properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(SmsResponse.class);

            if (response == null || !response.successful()) {
                String resultCode = "NO_BODY";
                if (response != null) {
                    resultCode = response.header() == null
                            ? "NO_HEADER"
                            : String.valueOf(response.header().resultCode());
                }
                log.warn("[SMS] 발송 거절 [purpose={} resultCode={}]", purpose, resultCode);
                return response == null || response.header() == null
                        ? NotificationSendResult.DELIVERY_UNKNOWN
                        : response.failureResult();
            }
            log.info("[SMS] 발송 성공 purpose={}", purpose);
            return NotificationSendResult.SUCCESS;
        } catch (RestClientResponseException e) {
            log.warn("[SMS] HTTP {} purpose={}", e.getStatusCode(), purpose);
            return NhnNotificationFailureClassifier.classify(e);
        } catch (ResourceAccessException e) {
            log.warn("[SMS] 네트워크 예외 [purpose={} type={}]", purpose, e.getClass().getSimpleName());
            return NhnNotificationFailureClassifier.classify(e);
        } catch (Exception e) {
            log.warn("[SMS] 발송 예외 [purpose={} type={}]", purpose, e.getClass().getSimpleName());
            return NotificationSendResult.DELIVERY_UNKNOWN;
        }
    }

    private static String correlationId(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        return "hg-" + UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8));
    }

    private record SmsResponse(SmsResponseHeader header) {
        private boolean successful() {
            return header != null && header.isSuccessful() && header.resultCode() == 0;
        }

        private NotificationSendResult failureResult() {
            return switch (header.resultCode()) {
                case -9999, -2021 -> NotificationSendResult.TRANSIENT_FAILURE;
                default -> NotificationSendResult.PERMANENT_FAILURE;
            };
        }
    }

    private record SmsResponseHeader(boolean isSuccessful, int resultCode) {}
}
