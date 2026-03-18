package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;

public record MyOrderSummary(Long orderId, String status, long totalAmount,
                              LocalDateTime paidAt, LocalDateTime createdAt) {
    public static MyOrderSummary from(Order o) {
        return new MyOrderSummary(o.getId(), o.getStatus().name(),
                o.getTotalAmount(), o.getPaidAt(), o.getCreatedAt());
    }
}
