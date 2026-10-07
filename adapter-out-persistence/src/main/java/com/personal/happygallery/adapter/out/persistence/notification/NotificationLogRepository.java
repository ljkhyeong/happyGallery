package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationChannel;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>,
        NotificationLogStorePort {

    @Override
    <S extends NotificationLog> S save(S log);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n
            FROM NotificationLog n
            WHERE n.channel = :channel
              AND n.providerRequestId = :providerRequestId
              AND n.providerRecipientSeq = :providerRecipientSeq
              AND n.status = com.personal.happygallery.domain.notification.NotificationLogStatus.REQUESTED
            """)
    Optional<NotificationLog> findRequestedForUpdate(
            @Param("channel") NotificationChannel channel,
            @Param("providerRequestId") String providerRequestId,
            @Param("providerRecipientSeq") Long providerRecipientSeq);
}
