package com.personal.happygallery.app.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 발송 기록 조회 포트 — 중복 발송 방지 및 알림 목록 조회에 사용.
 */
public interface NotificationLogReaderPort {

    boolean existsSentNotification(Long guestId, NotificationEventType eventType,
                                   LocalDateTime sentStart, LocalDateTime sentEnd);

    boolean existsSentUserNotification(Long userId, NotificationEventType eventType,
                                       LocalDateTime sentStart, LocalDateTime sentEnd);

    Optional<NotificationLog> findById(Long id);

    List<NotificationLog> findByUserIdOrderBySentAtDesc(Long userId, int limit, int offset);

    List<NotificationLog> findByGuestIdOrderBySentAtDesc(Long guestId, int limit, int offset);

    long countUnreadByUserId(Long userId);

    long countUnreadByGuestId(Long guestId);

    void markAllReadByUserId(Long userId, LocalDateTime now);

    void markAllReadByGuestId(Long guestId, LocalDateTime now);
}
