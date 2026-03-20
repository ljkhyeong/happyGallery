package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.order.port.in.OrderShippingUseCase.ShippingResult;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

/** 배송 관련 Admin 응답 */
public record ShippingResponse(
        Long orderId,
        OrderStatus status,
        LocalDate expectedShipDate
) {
    public static ShippingResponse from(ShippingResult result) {
        return new ShippingResponse(result.orderId(), result.status(), result.expectedShipDate());
    }
}
