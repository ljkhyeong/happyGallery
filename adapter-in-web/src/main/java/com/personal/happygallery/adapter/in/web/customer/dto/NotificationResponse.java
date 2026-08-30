package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase.NotificationView;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NotificationEventType eventType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String aggregateType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long aggregateId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime deliveredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime readAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean read) {
    public static NotificationResponse from(NotificationView v) {
        return new NotificationResponse(
                v.id(), v.eventType(), v.aggregateType(), v.aggregateId(),
                v.deliveredAt(), v.readAt(), v.isRead());
    }
}
