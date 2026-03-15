package com.personal.happygallery.app.notification;

import com.personal.happygallery.app.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.infra.notification.NotificationSender;

/**
 * {@link NotificationSender}(infra) → {@link NotificationSenderPort}(app) 브릿지 어댑터.
 *
 * <p>점진적 헥사고날 전환 중 app 서비스가 infra 구현을 직접 참조하지 않도록 중개한다.
 * 향후 infra 모듈이 app 포트를 직접 구현하게 되면 이 클래스는 제거된다.
 */
class NotificationSenderPortAdapter implements NotificationSenderPort {

    private final NotificationSender delegate;

    NotificationSenderPortAdapter(NotificationSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public NotificationChannel channel() {
        return delegate.channel();
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        return delegate.send(phone, recipientName, eventType);
    }
}
