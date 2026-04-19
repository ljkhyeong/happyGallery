package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationLog;

/**
 * 알림 발송 기록 저장 포트.
 */
public interface NotificationLogStorePort {

    NotificationLog save(NotificationLog log);
}
