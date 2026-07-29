package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record FailedNotificationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long outboxId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NotificationRecipientType recipientType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long recipientId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NotificationEventType eventType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String aggregateType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long aggregateId,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"FAILED", "PENDING"})
        NotificationOutboxStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int attemptCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String lastError,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
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
                createdAt.atOffset(ZoneOffset.UTC));
    }
}
