package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

/**
 * 배송 이행 관리 유스케이스.
 *
 * <p>배송 준비, 배송 출발, 배송 완료를 지원한다.
 */
public interface OrderShippingUseCase {

    record ShippingResult(Long orderId, OrderStatus status, LocalDate expectedShipDate) {
        public static ShippingResult of(Order order, Fulfillment fulfillment) {
            return new ShippingResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
        }
    }

    ShippingResult prepareShipping(Long orderId, Long adminId);

    ShippingResult markShipped(Long orderId, Long adminId);

    ShippingResult markDelivered(Long orderId, Long adminId);
}
