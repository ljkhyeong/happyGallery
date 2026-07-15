package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>,
        NotificationLogReaderPort, NotificationLogStorePort {

    @Override NotificationLog save(NotificationLog log);
    @Override Optional<NotificationLog> findById(Long id);

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

    @Query("SELECT n FROM NotificationLog n WHERE n.userId = :userId ORDER BY n.sentAt DESC")
    List<NotificationLog> findByUserIdOrderBySentAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM NotificationLog n WHERE n.guestId = :guestId ORDER BY n.sentAt DESC")
    List<NotificationLog> findByGuestIdOrderBySentAtDesc(@Param("guestId") Long guestId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    long countByGuestIdAndReadAtIsNull(Long guestId);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    void markAllReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.readAt = :readAt WHERE n.guestId = :guestId AND n.readAt IS NULL")
    void markAllReadByGuestId(@Param("guestId") Long guestId, @Param("readAt") LocalDateTime readAt);

    @Override
    default List<NotificationLog> findByUserIdOrderBySentAtDesc(Long userId, int limit, int offset) {
        return findByUserIdOrderBySentAtDesc(userId, PageRequest.of(offset / limit, limit));
    }

    @Override
    default List<NotificationLog> findByGuestIdOrderBySentAtDesc(Long guestId, int limit, int offset) {
        return findByGuestIdOrderBySentAtDesc(guestId, PageRequest.of(offset / limit, limit));
    }

    @Override
    default long countUnreadByUserId(Long userId) {
        return countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    default long countUnreadByGuestId(Long guestId) {
        return countByGuestIdAndReadAtIsNull(guestId);
    }
}
