package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        String accessToken,
        String status,
        long totalAmount,
        LocalDateTime paidAt
) {
    public static OrderResponse from(Order order, String rawAccessToken) {
        return new OrderResponse(
                order.getId(),
                rawAccessToken,
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getPaidAt()
        );
    }
}
