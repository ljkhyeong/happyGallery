package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>,
        NotificationLogReaderPort, NotificationLogStorePort {

    @Override NotificationLog save(NotificationLog log);

    @Override
    @Query("""
            SELECT DISTINCT n.guestId
            FROM NotificationLog n
            WHERE n.guestId IN :guestIds
              AND n.eventType = :eventType
              AND n.status = 'SUCCESS'
              AND n.sentAt BETWEEN :sentStart AND :sentEnd
            """)
    List<Long> findSentGuestIds(@Param("guestIds") List<Long> guestIds,
                                @Param("eventType") NotificationEventType eventType,
                                @Param("sentStart") LocalDateTime sentStart,
                                @Param("sentEnd") LocalDateTime sentEnd);

    @Override
    @Query("""
            SELECT DISTINCT n.userId
            FROM NotificationLog n
            WHERE n.userId IN :userIds
              AND n.eventType = :eventType
              AND n.status = 'SUCCESS'
              AND n.sentAt BETWEEN :sentStart AND :sentEnd
            """)
    List<Long> findSentUserIds(@Param("userIds") List<Long> userIds,
                               @Param("eventType") NotificationEventType eventType,
                               @Param("sentStart") LocalDateTime sentStart,
                               @Param("sentEnd") LocalDateTime sentEnd);

}
