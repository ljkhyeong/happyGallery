package com.personal.happygallery.app.notification;

import com.personal.happygallery.app.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.infra.notification.NotificationSender;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * infra {@link NotificationSender} 목록을 app {@link NotificationSenderPort} 목록으로 변환한다.
 *
 * <p>Spring이 {@code @Order} 우선순위로 정렬한 {@code List<NotificationSender>}를 받아
 * 동일 순서의 {@code List<NotificationSenderPort>}를 생성한다.
 */
@Configuration
class NotificationPortConfig {

    @Bean
    List<NotificationSenderPort> notificationSenderPorts(List<NotificationSender> senders) {
        return senders.stream()
                .<NotificationSenderPort>map(NotificationSenderPortAdapter::new)
                .toList();
    }
}
