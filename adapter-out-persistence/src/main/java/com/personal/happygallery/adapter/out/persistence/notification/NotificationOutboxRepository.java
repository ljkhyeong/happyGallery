package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long>, NotificationOutboxPort {

    @Override
    NotificationOutbox save(NotificationOutbox outbox);

    @Override
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n
            FROM NotificationOutbox n
            WHERE (n.status = com.personal.happygallery.domain.notification.NotificationOutboxStatus.PENDING
                   AND n.nextAttemptAt <= :now)
               OR (n.status = com.personal.happygallery.domain.notification.NotificationOutboxStatus.PROCESSING
                   AND n.lockedAt < :staleBefore)
            ORDER BY n.createdAt ASC, n.id ASC
            """)
    List<NotificationOutbox> findDispatchableForUpdate(@Param("now") LocalDateTime now,
                                                       @Param("staleBefore") LocalDateTime staleBefore,
                                                       Pageable pageable);

    @Override
    default List<NotificationOutbox> findDispatchable(LocalDateTime now, LocalDateTime staleBefore, int limit) {
        return findDispatchableForUpdate(
                now,
                staleBefore,
                PageRequest.of(0, limit));
    }
}
