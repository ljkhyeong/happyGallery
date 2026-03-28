package com.personal.happygallery.infra.notification;

import com.personal.happygallery.app.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, NotificationLogStorePort {

    @Override NotificationLog save(NotificationLog log);

    boolean existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(Long guestId,
                                                                 NotificationEventType eventType,
                                                                 String status,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end);

    boolean existsByUserIdAndEventTypeAndStatusAndSentAtBetween(Long userId,
                                                                NotificationEventType eventType,
                                                                String status,
                                                                LocalDateTime start,
                                                                LocalDateTime end);

    @Query("SELECT n FROM NotificationLog n WHERE n.userId = :userId ORDER BY n.sentAt DESC")
    List<NotificationLog> findByUserIdOrderBySentAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM NotificationLog n WHERE n.guestId = :guestId ORDER BY n.sentAt DESC")
    List<NotificationLog> findByGuestIdOrderBySentAtDesc(@Param("guestId") Long guestId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    long countByGuestIdAndReadAtIsNull(Long guestId);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    void markAllReadByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.readAt = :now WHERE n.guestId = :guestId AND n.readAt IS NULL")
    void markAllReadByGuestId(@Param("guestId") Long guestId, @Param("now") LocalDateTime now);
}
