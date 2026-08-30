package com.personal.happygallery.application.notification.port.out;

import java.time.LocalDateTime;

public interface NotificationRetentionPort {

    int deleteChannelLogsBefore(LocalDateTime cutoff, int limit);

    int deleteTerminalOutboxesBefore(LocalDateTime cutoff, int limit);
}
