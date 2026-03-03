package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;

/** 픽업 관련 Admin 응답 */
public record PickupResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime pickupDeadlineAt
) {}
