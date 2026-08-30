package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationRetentionPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationRetentionService {

    private final NotificationRetentionPort retentionPort;

    public NotificationRetentionService(NotificationRetentionPort retentionPort) {
        this.retentionPort = retentionPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteChannelLogsBefore(LocalDateTime cutoff, int batchSize) {
        return retentionPort.deleteChannelLogsBefore(cutoff, batchSize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteTerminalOutboxesBefore(LocalDateTime cutoff, int batchSize) {
        return retentionPort.deleteTerminalOutboxesBefore(cutoff, batchSize);
    }
}
