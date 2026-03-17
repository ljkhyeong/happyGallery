package com.personal.happygallery.app.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;

/**
 * 알림 발송 기록 조회 포트 — 중복 발송 방지 등에 사용.
 */
public interface NotificationLogReaderPort {

    boolean existsSentNotification(Long guestId, NotificationEventType eventType,
                                   LocalDateTime sentStart, LocalDateTime sentEnd);
}
