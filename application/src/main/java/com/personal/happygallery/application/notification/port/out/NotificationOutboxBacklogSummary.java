package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import java.time.LocalDateTime;

public record NotificationOutboxBacklogSummary(
        NotificationOutboxStatus status,
        long count,
        LocalDateTime oldestActionAt
) {}
