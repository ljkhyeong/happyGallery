package com.personal.happygallery.infra.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 카카오 알림톡 실제 발송 어댑터.
 * prod 프로필에서만 활성화된다.
 */
@Component
@Order(1)
@Profile("prod")
public class KakaoAlimtalkSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(KakaoAlimtalkSender.class);

    private final KakaoNotificationProperties properties;
    private final HttpClient httpClient;

    public KakaoAlimtalkSender(KakaoNotificationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.timeoutMillis()))
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        try {
            String templateCode = resolveTemplateCode(eventType);
            String body = """
                    {"senderKey":"%s","templateCode":"%s","recipientNo":"%s","templateArgs":{"name":"%s"}}"""
                    .formatted(properties.senderKey(), templateCode, phone, recipientName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl() + "/v1/api/talk/friends/message/default/send"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "KakaoAK " + properties.apiKey())
                    .timeout(Duration.ofMillis(properties.timeoutMillis()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.info("[KAKAO] 발송 성공 phone={} event={}", phone, eventType);
            } else {
                log.warn("[KAKAO] HTTP {} phone={} event={} body={}", response.statusCode(), phone, eventType, response.body());
            }
            return success;
        } catch (Exception e) {
            log.warn("[KAKAO] 발송 예외 phone={} event={}", phone, eventType, e);
            return false;
        }
    }

    private String resolveTemplateCode(NotificationEventType eventType) {
        return switch (eventType) {
            case BOOKING_CONFIRMED -> "HG_BOOKING_CONFIRMED";
            case BOOKING_RESCHEDULED -> "HG_BOOKING_RESCHEDULED";
            case BOOKING_CANCELED -> "HG_BOOKING_CANCELED";
            case DEPOSIT_REFUNDED -> "HG_DEPOSIT_REFUNDED";
            case ORDER_PAID -> "HG_ORDER_PAID";
            case ORDER_REFUNDED -> "HG_ORDER_REFUNDED";
            case REMINDER_D1 -> "HG_REMINDER_D1";
            case REMINDER_SAME_DAY -> "HG_REMINDER_SAME_DAY";
            case PASS_EXPIRY_SOON -> "HG_PASS_EXPIRY_SOON";
            case PICKUP_DEADLINE_REMINDER -> "HG_PICKUP_DEADLINE";
        };
    }
}
