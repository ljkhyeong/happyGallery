package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendOutcome;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * 개발용 가짜 카카오·SMS 알림 어댑터.
 * 항상 성공 응답을 반환한다. prod 프로필에서는 실제 어댑터가 활성화된다.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!prod")
class FakeNotificationSenderConfig {

    private static final Logger log = LoggerFactory.getLogger(FakeNotificationSenderConfig.class);

    @Bean
    @Order(1)
    NotificationSenderPort fakeKakaoSender() {
        return new FakeNotificationSender(NotificationChannel.KAKAO);
    }

    @Bean
    @Order(2)
    NotificationSenderPort fakeSmsSender() {
        return new FakeNotificationSender(NotificationChannel.SMS);
    }

    private record FakeNotificationSender(NotificationChannel channel) implements NotificationSenderPort {
        @Override
        public NotificationSendOutcome send(String idempotencyKey, String phone,
                                             String recipientName, NotificationEventType eventType) {
            log.info("[FAKE-{}] event={}", channel, eventType);
            return NotificationSendOutcome.immediate(NotificationSendResult.SUCCESS);
        }
    }
}
