package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

/** 예약 제작 관련 Admin 응답 */
public record OrderProductionResponse(
        Long orderId,
        OrderStatus status,
        LocalDate expectedShipDate
) {
    public static OrderProductionResponse of(Order order, LocalDate expectedShipDate) {
        return new OrderProductionResponse(order.getId(), order.getStatus(), expectedShipDate);
    }

    public static OrderProductionResponse of(Order order) {
        return new OrderProductionResponse(order.getId(), order.getStatus(), null);
    }
}
