package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.order.OrderProductionService.ProductionResult;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

/** 예약 제작 관련 Admin 응답 */
public record OrderProductionResponse(
        Long orderId,
        OrderStatus status,
        LocalDate expectedShipDate
) {
    public static OrderProductionResponse from(ProductionResult result) {
        return new OrderProductionResponse(result.orderId(), result.status(), result.expectedShipDate());
    }
}
