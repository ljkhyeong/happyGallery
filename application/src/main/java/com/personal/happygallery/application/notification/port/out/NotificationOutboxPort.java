package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxPort {

    <S extends NotificationOutbox> S save(S outbox);

    Optional<NotificationOutbox> findById(Long id);

    Optional<NotificationOutbox> findByIdForUpdate(Long id);

    Optional<NotificationOutbox> findByIdempotencyKeyForUpdate(String idempotencyKey);

    List<NotificationOutbox> findDispatchable(LocalDateTime now, LocalDateTime staleBefore, int limit);

    List<NotificationOutbox> findDeliveryResultCheckable(
            LocalDateTime now, LocalDateTime staleBefore, int limit);

    List<NotificationOutbox> findFailed(int limit);

    List<NotificationOutbox> findSentByUserId(Long userId, boolean unreadOnly, int page, int size);

    long countUnreadSentByUserId(Long userId);

    void markAllSentReadByUserId(Long userId, LocalDateTime readAt);

    List<NotificationOutboxBacklogSummary> summarizeUnresolvedBacklog();
}
