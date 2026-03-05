package com.personal.happygallery.infra.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 개발용 가짜 SMS 어댑터.
 * 항상 성공 응답을 반환한다. 실제 SMS 연동 시 교체해야 한다.
 */
@Component
@Order(2)
public class FakeSmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FakeSmsSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        log.info("[FAKE-SMS] phone={} event={} recipient={}", phone, eventType, recipientName);
        return true;
    }
}
