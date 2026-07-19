package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.notification.dto.SmsRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** NHN Cloud SMS 요청 형식과 성공 판정을 한 곳에서 관리한다. */
final class NhnSmsClient {

    private static final Logger log = LoggerFactory.getLogger(NhnSmsClient.class);

    private final SmsNotificationProperties properties;
    private final RestClient restClient;

    NhnSmsClient(SmsNotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    boolean send(String phone, String message, String purpose) {
        return send(phone, message, purpose, "/sender/sms");
    }

    boolean sendVerification(String phone, String message) {
        return send(phone, message, "PHONE_VERIFICATION", "/sender/auth/sms");
    }

    private boolean send(String phone, String message, String purpose, String senderPath) {
        try {
            SmsRequest request = new SmsRequest(
                    message,
                    properties.senderNumber(),
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
                return false;
            }
            log.info("[SMS] 발송 성공 purpose={}", purpose);
            return true;
        } catch (RestClientResponseException e) {
            log.warn("[SMS] HTTP {} purpose={}", e.getStatusCode(), purpose);
            return false;
        } catch (Exception e) {
            log.warn("[SMS] 발송 예외 [purpose={} type={}]", purpose, e.getClass().getSimpleName());
            return false;
        }
    }

    private record SmsResponse(SmsResponseHeader header) {
        private boolean successful() {
            return header != null && header.isSuccessful() && header.resultCode() == 0;
        }
    }

    private record SmsResponseHeader(boolean isSuccessful, int resultCode) {}
}
