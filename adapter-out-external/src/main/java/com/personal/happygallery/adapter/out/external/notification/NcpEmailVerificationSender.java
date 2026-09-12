package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

/** 한국 리전 Cloud Outbound Mailer에 인증 메일 한 건의 발송을 요청한다. */
final class NcpEmailVerificationSender implements EmailVerificationTransport {

    private static final String PATH = "/api/v1/mails";
    private static final String ENDPOINT = "https://mail.apigw.ntruss.com" + PATH;

    private final RestClient client;
    private final NcpMailProperties credentials;
    private final EmailVerificationProperties properties;
    private final Clock clock;

    NcpEmailVerificationSender(RestClient client, NcpMailProperties credentials,
                               EmailVerificationProperties properties, Clock clock) {
        this.client = client;
        this.credentials = credentials;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public NotificationSendResult sendResult(String email, String verificationCode) {
        String timestamp = Long.toString(clock.millis());
        Map<String, Object> request = Map.of(
                "senderAddress", properties.from(),
                "title", properties.subject(),
                "body", HtmlUtils.htmlEscape(EmailVerificationMessage.body(verificationCode)).replace("\n", "<br>"),
                "recipients", List.of(Map.of("address", email, "type", "R")),
                "individual", true,
                "advertising", false,
                "confirmAndSend", false);
        return client.post().uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", credentials.accessKey())
                .header("x-ncp-apigw-signature-v2", signature(timestamp))
                .body(request)
                .exchange((sent, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        MailResponse body = response.bodyTo(MailResponse.class);
                        // 접수 성공이며 수신함 도착까지 보장하는 결과는 아니다.
                        return body != null && StringUtils.hasText(body.requestId()) && Integer.valueOf(1).equals(body.count())
                                ? NotificationSendResult.SUCCESS : NotificationSendResult.DELIVERY_UNKNOWN;
                    }
                    int status = response.getStatusCode().value();
                    if (status == 429) {
                        return NotificationSendResult.TRANSIENT_FAILURE;
                    }
                    if (response.getStatusCode().is4xxClientError() && status != 408) {
                        return NotificationSendResult.PERMANENT_FAILURE;
                    }
                    return NotificationSendResult.DELIVERY_UNKNOWN;
                });
    }

    private String signature(String timestamp) {
        String message = "POST " + PATH + "\n" + timestamp + "\n" + credentials.accessKey();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(credentials.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("네이버 메일 API 서명을 생성할 수 없습니다.", exception);
        }
    }

    private record MailResponse(String requestId, Integer count) {}
}
