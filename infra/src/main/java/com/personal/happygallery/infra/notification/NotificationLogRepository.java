package com.personal.happygallery.infra.notification;

import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.app.notification.port.out.NotificationLogStorePort;
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

    String SUCCESS_STATUS = "SUCCESS";

    @Override NotificationLog save(NotificationLog log);
    @Override Optional<NotificationLog> findById(Long id);

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

    @Override
    default boolean existsSentNotification(Long guestId, NotificationEventType eventType,
                                           LocalDateTime sentStart, LocalDateTime sentEnd) {
        return existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(
                guestId, eventType, SUCCESS_STATUS, sentStart, sentEnd);
    }

    @Override
    default boolean existsSentUserNotification(Long userId, NotificationEventType eventType,
                                               LocalDateTime sentStart, LocalDateTime sentEnd) {
        return existsByUserIdAndEventTypeAndStatusAndSentAtBetween(
                userId, eventType, SUCCESS_STATUS, sentStart, sentEnd);
    }

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
