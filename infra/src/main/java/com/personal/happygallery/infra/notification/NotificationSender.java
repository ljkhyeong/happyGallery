package com.personal.happygallery.infra.notification;

import com.personal.happygallery.app.notification.port.out.NotificationSenderPort;

/**
 * 알림 발송 인프라 인터페이스.
 * {@link NotificationSenderPort}를 확장하여 infra 구현체가 app 포트를 자동으로 만족한다.
 * 구현체: {@link FakeKakaoSender}, {@link FakeSmsSender} (개발·테스트), 실제 연동 어댑터 (추후).
 */
public interface NotificationSender extends NotificationSenderPort {
}
