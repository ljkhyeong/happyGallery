package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;

/**
 * 픽업 이행 관리 유스케이스.
 *
 * <p>픽업 준비 완료와 픽업 완료 처리를 지원한다.
 */
public interface OrderPickupUseCase {

    record PickupResult(Long orderId, OrderStatus status, LocalDateTime pickupDeadlineAt) {
        public static PickupResult of(Order order, Fulfillment fulfillment) {
            return new PickupResult(order.getId(), order.getStatus(), fulfillment.getPickupDeadlineAt());
        }
    }

    PickupResult markPickupReady(Long orderId, LocalDateTime pickupDeadlineAt);

    PickupResult confirmPickup(Long orderId);
}
