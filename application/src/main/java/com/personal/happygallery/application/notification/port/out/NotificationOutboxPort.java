package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxPort {

    NotificationOutbox save(NotificationOutbox outbox);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<NotificationOutbox> findById(Long id);

    Optional<NotificationOutbox> findByIdForUpdate(Long id);

    List<NotificationOutbox> findDispatchable(LocalDateTime now, LocalDateTime staleBefore, int limit);

    List<NotificationOutbox> findFailed(int limit);

}
