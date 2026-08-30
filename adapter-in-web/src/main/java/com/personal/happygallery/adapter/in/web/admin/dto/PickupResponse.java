package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.OrderPickupUseCase.PickupResult;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 픽업 관련 Admin 응답 */
public record PickupResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime pickupDeadlineAt
) {
    public static PickupResponse from(PickupResult result) {
        return new PickupResponse(result.orderId(), result.status(), result.pickupDeadlineAt());
    }
}
