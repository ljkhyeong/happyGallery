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

    List<NotificationOutbox> findFailed(int limit);

    List<NotificationOutbox> findSentByUserId(Long userId, int limit, int offset);

    List<NotificationOutbox> findSentByGuestId(Long guestId, int limit, int offset);

    long countUnreadSentByUserId(Long userId);

    long countUnreadSentByGuestId(Long guestId);

    void markAllSentReadByUserId(Long userId, LocalDateTime readAt);

    void markAllSentReadByGuestId(Long guestId, LocalDateTime readAt);

    List<NotificationOutboxBacklogSummary> summarizeUnresolvedBacklog();
}
