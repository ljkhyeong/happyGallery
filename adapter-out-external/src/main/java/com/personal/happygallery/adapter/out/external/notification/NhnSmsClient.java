package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.notification.dto.SmsRequest;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSendOutcome;
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
        return sendTracked(idempotencyKey, phone, message, purpose).result();
    }

    NotificationSendOutcome sendTracked(
            String idempotencyKey, String phone, String message, String purpose) {
        return send(idempotencyKey, phone, message, purpose, "/sender/sms", true);
    }

    NotificationSendResult sendVerification(String phone, String message) {
        return send(null, phone, message, "PHONE_VERIFICATION", "/sender/auth/sms", false).result();
    }

    private NotificationSendOutcome send(String idempotencyKey,
                                        String phone,
                                        String message,
                                        String purpose,
                                        String senderPath,
                                        boolean trackResult) {
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

            ResponseOutcome outcome = response == null
                    ? ResponseOutcome.immediate(NotificationSendResult.DELIVERY_UNKNOWN, "NO_BODY")
                    : response.outcome(trackResult);
            if (outcome.result() != NotificationSendResult.ACCEPTED) {
                if (outcome.result() != NotificationSendResult.SUCCESS) {
                    log.warn("[SMS] 발송 거절 [purpose={} resultCode={}]",
                            purpose, outcome.diagnosticCode());
                }
                return NotificationSendOutcome.immediate(outcome.result());
            }
            log.info("[SMS] 발송 요청 접수 purpose={}", purpose);
            return trackResult
                    ? NotificationSendOutcome.accepted(outcome.requestId(), outcome.recipientSeq())
                    : NotificationSendOutcome.immediate(NotificationSendResult.SUCCESS);
        } catch (RestClientResponseException e) {
            log.warn("[SMS] HTTP {} purpose={}", e.getStatusCode(), purpose);
            return NotificationSendOutcome.immediate(NhnNotificationFailureClassifier.classify(e));
        } catch (ResourceAccessException e) {
            log.warn("[SMS] 네트워크 예외 [purpose={} type={}]", purpose, e.getClass().getSimpleName());
            return NotificationSendOutcome.immediate(NhnNotificationFailureClassifier.classify(e));
        } catch (Exception e) {
            log.warn("[SMS] 발송 예외 [purpose={} type={}]",
                    purpose, e.getClass().getSimpleName());
            return NotificationSendOutcome.immediate(NotificationSendResult.DELIVERY_UNKNOWN);
        }
    }

    private static String correlationId(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        return "hg-" + UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8));
    }

    private record SmsResponse(SmsResponseHeader header, SmsResponseBody body) {
        private ResponseOutcome outcome(boolean trackResult) {
            if (header == null) {
                return ResponseOutcome.immediate(NotificationSendResult.DELIVERY_UNKNOWN, "NO_HEADER");
            }
            int resultCode = header.resultCode();
            if (header.isSuccessful() && resultCode == 0) {
                if (!trackResult) {
                    return ResponseOutcome.immediate(NotificationSendResult.SUCCESS, "0");
                }
                if (body == null || body.data() == null
                        || body.data().requestId() == null
                        || body.data().sendResultList() == null
                        || body.data().sendResultList().size() != 1
                        || body.data().sendResultList().getFirst().recipientSeq() == null) {
                    return ResponseOutcome.immediate(NotificationSendResult.DELIVERY_UNKNOWN, "NO_SEND_RESULT");
                }
                SmsSendResult sendResult = body.data().sendResultList().getFirst();
                if (sendResult.resultCode() != 0) {
                    return ResponseOutcome.immediate(
                            classifyResultCode(sendResult.resultCode()),
                            String.valueOf(sendResult.resultCode()));
                }
                return new ResponseOutcome(
                        NotificationSendResult.ACCEPTED,
                        "0",
                        body.data().requestId(),
                        sendResult.recipientSeq());
            }
            return ResponseOutcome.immediate(
                    classifyResultCode(resultCode), String.valueOf(resultCode));
        }
    }

    private static NotificationSendResult classifyResultCode(int resultCode) {
        return switch (resultCode) {
            case -9999, -2021 -> NotificationSendResult.TRANSIENT_FAILURE;
            default -> NotificationSendResult.PERMANENT_FAILURE;
        };
    }

    private record ResponseOutcome(
            NotificationSendResult result,
            String diagnosticCode,
            String requestId,
            Long recipientSeq
    ) {
        private static ResponseOutcome immediate(
                NotificationSendResult result, String diagnosticCode) {
            return new ResponseOutcome(result, diagnosticCode, null, null);
        }
    }

    private record SmsResponseHeader(boolean isSuccessful, int resultCode) {}

    private record SmsResponseBody(SmsResponseData data) {}

    private record SmsResponseData(String requestId, List<SmsSendResult> sendResultList) {}

    private record SmsSendResult(int resultCode, Long recipientSeq) {}
}
