package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;
import java.util.List;

public record MyOrderSummary(Long orderId, String status, long totalAmount,
                              LocalDateTime paidAt, LocalDateTime createdAt) {
    public static MyOrderSummary from(Order o) {
        return new MyOrderSummary(o.getId(), o.getStatus().name(),
                o.getTotalAmount(), o.getPaidAt(), o.getCreatedAt());
    }

    public static List<MyOrderSummary> fromAll(List<Order> orders) {
        return orders.stream().map(MyOrderSummary::from).toList();
    }
}
