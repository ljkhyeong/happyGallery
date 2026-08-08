package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxBacklogSummary;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaNotificationPersistenceAdapter implements NotificationLogStorePort, NotificationOutboxPort {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;

    JpaNotificationPersistenceAdapter(
            NotificationLogRepository notificationLogRepository,
            NotificationOutboxRepository notificationOutboxRepository) {
        this.notificationLogRepository = notificationLogRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
    }

    @Override
    public NotificationLog save(NotificationLog log) {
        return notificationLogRepository.save(log);
    }

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        return notificationOutboxRepository.save(outbox);
    }

    @Override
    public Optional<NotificationOutbox> findById(Long id) {
        return notificationOutboxRepository.findById(id);
    }

    @Override
    public Optional<NotificationOutbox> findByIdForUpdate(Long id) {
        return notificationOutboxRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<NotificationOutbox> findByIdempotencyKeyForUpdate(String idempotencyKey) {
        return notificationOutboxRepository.findByIdempotencyKeyForUpdate(idempotencyKey);
    }

    @Override
    public List<NotificationOutbox> findDispatchable(
            LocalDateTime now, LocalDateTime staleBefore, int limit) {
        return notificationOutboxRepository.findDispatchable(now, staleBefore, limit);
    }

    @Override
    public List<NotificationOutbox> findFailed(int limit) {
        return notificationOutboxRepository.findFailed(limit);
    }

    @Override
    public List<NotificationOutbox> findSentByUserId(Long userId, int limit, int offset) {
        return notificationOutboxRepository.findSentByUserId(userId, limit, offset);
    }

    @Override
    public List<NotificationOutbox> findSentByGuestId(Long guestId, int limit, int offset) {
        return notificationOutboxRepository.findSentByGuestId(guestId, limit, offset);
    }

    @Override
    public long countUnreadSentByUserId(Long userId) {
        return notificationOutboxRepository.countUnreadSentByUserId(userId);
    }

    @Override
    public long countUnreadSentByGuestId(Long guestId) {
        return notificationOutboxRepository.countUnreadSentByGuestId(guestId);
    }

    @Override
    public void markAllSentReadByUserId(Long userId, LocalDateTime readAt) {
        notificationOutboxRepository.markAllSentReadByUserId(userId, readAt);
    }

    @Override
    public void markAllSentReadByGuestId(Long guestId, LocalDateTime readAt) {
        notificationOutboxRepository.markAllSentReadByGuestId(guestId, readAt);
    }

    @Override
    public List<NotificationOutboxBacklogSummary> summarizeUnresolvedBacklog() {
        return notificationOutboxRepository.summarizeUnresolvedBacklog();
    }
}
