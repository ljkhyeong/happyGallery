package com.personal.happygallery.infra.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;

/**
 * 알림 발송 포트 (Port).
 * 구현체: {@link FakeKakaoSender}, {@link FakeSmsSender} (개발·테스트), 실제 연동 어댑터 (추후).
 */
public interface NotificationSender {

    /** 이 Sender가 담당하는 채널 */
    NotificationChannel channel();

    /**
     * 알림을 발송한다.
     *
     * @param phone         수신자 전화번호
     * @param recipientName 수신자 이름
     * @param eventType     발송 이벤트 유형
     * @return 발송 성공 여부
     */
    boolean send(String phone, String recipientName, NotificationEventType eventType);
}
