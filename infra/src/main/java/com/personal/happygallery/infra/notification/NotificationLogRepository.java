package com.personal.happygallery.infra.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(Long guestId,
                                                                 NotificationEventType eventType,
                                                                 String status,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end);
}
