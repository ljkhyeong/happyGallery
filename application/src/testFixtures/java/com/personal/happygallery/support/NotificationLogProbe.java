package com.personal.happygallery.support;

import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.application.notification.port.out.NotificationLogReaderPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationLogProbe {

    private final NotificationLogReaderPort notificationLogReaderPort;

    public NotificationLogProbe(NotificationLogReaderPort notificationLogReaderPort) {
        this.notificationLogReaderPort = notificationLogReaderPort;
    }

    public List<NotificationLog> all() {
        return notificationLogReaderPort.findAll();
    }
}
