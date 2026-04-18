package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.OrderPickupUseCase.PickupResult;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;

/** 픽업 관련 Admin 응답 */
public record PickupResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime pickupDeadlineAt
) {
    public static PickupResponse from(PickupResult result) {
        return new PickupResponse(result.orderId(), result.status(), result.pickupDeadlineAt());
    }
}
