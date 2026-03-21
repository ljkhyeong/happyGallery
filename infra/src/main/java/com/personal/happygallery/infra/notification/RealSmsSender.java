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
 * SMS 실제 발송 어댑터 (NHN Cloud SMS).
 * prod 프로필에서만 활성화된다.
 */
@Component
@Order(2)
@Profile("prod")
public class RealSmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(RealSmsSender.class);

    private final SmsNotificationProperties properties;
    private final HttpClient httpClient;

    public RealSmsSender(SmsNotificationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.timeoutMillis()))
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        try {
            String message = buildMessage(recipientName, eventType);

            String body = """
                    {"body":"%s","sendNo":"%s","recipientList":[{"recipientNo":"%s"}]}"""
                    .formatted(escapeJson(message), properties.senderNumber(), phone);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl() + "/sms/v3.0/appKeys/" + properties.apiKey() + "/sender/sms"))
                    .header("Content-Type", "application/json")
                    .header("X-Secret-Key", properties.apiSecret())
                    .timeout(Duration.ofMillis(properties.timeoutMillis()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.info("[SMS] 발송 성공 phone={} event={}", phone, eventType);
            } else {
                log.warn("[SMS] HTTP {} phone={} event={} body={}", response.statusCode(), phone, eventType, response.body());
            }
            return success;
        } catch (Exception e) {
            log.warn("[SMS] 발송 예외 phone={} event={}", phone, eventType, e);
            return false;
        }
    }

    private String buildMessage(String recipientName, NotificationEventType eventType) {
        String prefix = "[해피갤러리] ";
        return switch (eventType) {
            case BOOKING_CONFIRMED -> prefix + recipientName + "님, 예약이 확정되었습니다.";
            case BOOKING_RESCHEDULED -> prefix + recipientName + "님, 예약이 변경되었습니다.";
            case BOOKING_CANCELED -> prefix + recipientName + "님, 예약이 취소되었습니다.";
            case DEPOSIT_REFUNDED -> prefix + recipientName + "님, 예약금이 환불되었습니다.";
            case ORDER_PAID -> prefix + recipientName + "님, 주문 결제가 완료되었습니다.";
            case ORDER_REFUNDED -> prefix + recipientName + "님, 주문이 환불되었습니다.";
            case REMINDER_D1 -> prefix + recipientName + "님, 내일 체험이 예정되어 있습니다.";
            case REMINDER_SAME_DAY -> prefix + recipientName + "님, 오늘 체험이 예정되어 있습니다.";
            case PASS_EXPIRY_SOON -> prefix + recipientName + "님, 8회권 만료가 7일 남았습니다.";
            case PICKUP_DEADLINE_REMINDER -> prefix + recipientName + "님, 픽업 마감이 2시간 남았습니다.";
        };
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
