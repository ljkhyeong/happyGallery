package com.personal.happygallery.infra.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 개발용 가짜 카카오 알림 어댑터.
 * 항상 성공 응답을 반환한다. prod 프로필에서는 실제 어댑터가 활성화된다.
 */
@Component
@Order(1)
@Profile("!prod")
public class FakeKakaoSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FakeKakaoSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        log.info("[FAKE-KAKAO] phone={} event={} recipient={}", phone, eventType, recipientName);
        return true;
    }
}
