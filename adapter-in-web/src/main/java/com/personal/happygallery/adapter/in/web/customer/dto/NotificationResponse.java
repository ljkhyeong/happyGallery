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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean read,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                description = "본인 주문의 구매 당시 상품명, 현재 예약 클래스 또는 재입고 상품·옵션. 원본이 없거나 타인 소유이면 null")
        String contextTitle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                description = "예약 알림 원본의 현재 예약일시. 알림 발생 당시 일정이 아니며 예약이 아니거나 원본을 조회할 수 없으면 null")
        LocalDateTime scheduledAt) {
    public static NotificationResponse from(NotificationView v) {
        return new NotificationResponse(
                v.id(), v.eventType(), v.aggregateType(), v.aggregateId(),
                v.deliveredAt(), v.readAt(), v.isRead(), v.contextTitle(), v.scheduledAt());
    }
}
