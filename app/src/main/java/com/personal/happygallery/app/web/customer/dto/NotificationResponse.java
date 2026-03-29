package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.app.notification.port.in.NotificationQueryUseCase.NotificationView;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, String channel, String eventType,
                                   String status, LocalDateTime sentAt,
                                   LocalDateTime readAt, boolean read) {
    public static NotificationResponse from(NotificationView v) {
        return new NotificationResponse(
                v.id(), v.channel().name(), v.eventType().name(),
                v.status(), v.sentAt(), v.readAt(), v.isRead());
    }
}
