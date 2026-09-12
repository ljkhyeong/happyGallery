package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/** 심사 대기 중에도 운영 보안 설정을 유지하며 발송만 중지한다. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
@EnableConfigurationProperties(NotificationDeliveryProperties.class)
class NotificationDeliveryConfig {

    NotificationDeliveryConfig(NotificationDeliveryProperties properties) {
        if ("disabled".equals(properties.mode())) {
            LoggerFactory.getLogger(NotificationDeliveryConfig.class).warn(
                    "[알림] NOTIFICATION_MODE=disabled: 휴대폰 인증과 고객 알림을 발송하지 않습니다. "
                            + "고객 서비스 개시 전에 nhn 모드로 전환하세요.");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("prod")
    @ConditionalOnProperty(prefix = "app.external.notification", name = "mode", havingValue = "disabled")
    static class Disabled {
        @Bean
        @Order(1)
        NotificationSenderPort disabledKakaoSender() {
            return new DisabledNotificationSender(NotificationChannel.KAKAO);
        }

        @Bean
        @Order(2)
        NotificationSenderPort disabledSmsSender() {
            return new DisabledNotificationSender(NotificationChannel.SMS);
        }

        @Bean
        PhoneVerificationSender disabledPhoneVerificationSender() {
            return (phone, verificationCode) -> false;
        }
    }

    private record DisabledNotificationSender(NotificationChannel channel) implements NotificationSenderPort {
        @Override
        public NotificationSendResult send(String idempotencyKey, String phone,
                                            String recipientName, NotificationEventType eventType) {
            return NotificationSendResult.PERMANENT_FAILURE;
        }
    }
}
