package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase.NotificationTarget;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase.NotificationView;
import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NotificationEventType eventType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) NotificationAggregateType aggregateType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long aggregateId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime deliveredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime readAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean read,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                description = "본인 주문의 구매 당시 상품명, 현재 예약 클래스, 빈자리 알림 클래스, 재입고 상품·옵션 또는 문의한 상품. 원본이 없거나 타인 소유이면 null")
        String contextTitle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                description = "예약 알림은 현재 예약일시, 빈자리 알림은 신청한 회차 시작 시각. 알림 발생 당시 일정이 아니며 그 밖의 알림이거나 원본을 조회할 수 없으면 null")
        LocalDateTime scheduledAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "알림을 눌렀을 때 열 본인 기록. 원본이 없거나 타인 소유이면 모든 값이 null")
        Target target) {
    public static NotificationResponse from(NotificationView v) {
        return new NotificationResponse(
                v.id(), v.eventType(), v.aggregateType(), v.aggregateId(),
                v.deliveredAt(), v.readAt(), v.isRead(), v.contextTitle(), v.scheduledAt(),
                Target.from(v.target()));
    }

    @Schema(name = "NotificationTarget")
    public record Target(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "주문·주문 클레임·주문 환불 알림의 주문 ID")
            Long orderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "예약·예약금 환불 알림의 예약 ID")
            Long bookingId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "재입고·상품 문의 답변 알림의 상품 ID")
            Long productId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "재입고 알림을 신청한 옵션 조합 ID. 상품 전체로 신청했으면 null")
            Long productVariantId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "빈자리 알림 회차의 클래스 ID")
            Long classId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "빈자리 알림 회차 ID")
            Long slotId) {
        private static Target from(NotificationTarget target) {
            return new Target(target.orderId(), target.bookingId(), target.productId(),
                    target.productVariantId(), target.classId(), target.slotId());
        }
    }
}
