package com.personal.happygallery.app.notification;

import com.personal.happygallery.app.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationLogReaderPort logReader;
    private final Clock clock;

    public NotificationQueryService(NotificationLogReaderPort logReader, Clock clock) {
        this.logReader = logReader;
        this.clock = clock;
    }

    @Override
    public List<NotificationView> listNotifications(Long userId, Long guestId, int page, int size) {
        int offset = page * size;
        List<NotificationLog> logs = (userId != null)
                ? logReader.findByUserIdOrderBySentAtDesc(userId, size, offset)
                : logReader.findByGuestIdOrderBySentAtDesc(guestId, size, offset);

        return logs.stream()
                .map(log -> new NotificationView(
                        log.getId(), log.getChannel(), log.getEventType(),
                        log.getStatus(), log.getSentAt(), log.getReadAt()))
                .toList();
    }

    @Override
    public long countUnread(Long userId, Long guestId) {
        return (userId != null)
                ? logReader.countUnreadByUserId(userId)
                : logReader.countUnreadByGuestId(guestId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId, Long guestId) {
        NotificationLog log = logReader.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        boolean isOwner = (userId != null && userId.equals(log.getUserId()))
                || (guestId != null && guestId.equals(log.getGuestId()));
        if (!isOwner) {
            throw new IllegalArgumentException("알림을 찾을 수 없습니다.");
        }

        log.markRead(LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId, Long guestId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (userId != null) {
            logReader.markAllReadByUserId(userId, now);
        } else {
            logReader.markAllReadByGuestId(guestId, now);
        }
    }
}
