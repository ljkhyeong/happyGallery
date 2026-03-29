package com.personal.happygallery.support;

import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationLogProbe {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationLogProbe(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    public List<NotificationLog> all() {
        return notificationLogRepository.findAll();
    }
}
