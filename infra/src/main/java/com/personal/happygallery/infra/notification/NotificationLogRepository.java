package com.personal.happygallery.infra.notification;

import com.personal.happygallery.domain.notification.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
