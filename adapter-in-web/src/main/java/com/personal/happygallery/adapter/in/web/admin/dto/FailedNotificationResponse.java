package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record FailedNotificationResponse(
        Long outboxId,
        NotificationRecipientType recipientType,
        Long recipientId,
        NotificationEventType eventType,
        String aggregateType,
        Long aggregateId,
        NotificationOutboxStatus status,
        int attemptCount,
        String lastError,
        OffsetDateTime createdAt
) {

    public static FailedNotificationResponse from(NotificationOutbox outbox) {
        Long recipientId = outbox.getGuestId() != null ? outbox.getGuestId() : outbox.getUserId();
        LocalDateTime createdAt = outbox.getCreatedAt();
        return new FailedNotificationResponse(
                outbox.getId(),
                outbox.getRecipientType(),
                recipientId,
                outbox.getEventType(),
                outbox.getAggregateType(),
                outbox.getAggregateId(),
                outbox.getStatus(),
                outbox.getAttemptCount(),
                outbox.getLastError(),
                createdAt == null ? null : createdAt.atOffset(ZoneOffset.UTC));
    }
}
