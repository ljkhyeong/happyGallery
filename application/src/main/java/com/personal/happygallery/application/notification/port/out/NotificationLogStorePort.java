package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationChannel;
import java.util.Optional;

/**
 * 알림 발송 기록 저장 포트.
 */
public interface NotificationLogStorePort {

    <S extends NotificationLog> S save(S log);

    Optional<NotificationLog> findRequestedForUpdate(
            NotificationChannel channel, String providerRequestId, Long providerRecipientSeq);
}
