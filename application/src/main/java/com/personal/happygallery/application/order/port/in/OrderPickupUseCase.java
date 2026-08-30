package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;

/**
 * 픽업 이행 관리 유스케이스.
 *
 * <p>픽업 준비 완료, 픽업 완료, 미수령 주문의 관리자 예외 환불을 지원한다.
 */
public interface OrderPickupUseCase {

    record PickupResult(Long orderId, OrderStatus status, LocalDateTime pickupDeadlineAt) {
        public static PickupResult of(Order order, Fulfillment fulfillment) {
            return new PickupResult(order.getId(), order.getStatus(), fulfillment.getPickupDeadlineAt());
        }
    }

    record MissedPickupRefundResult(Order order, Refund refund) {}

    PickupResult markPickupReady(Long orderId, LocalDateTime pickupDeadlineAt, Long adminId);

    PickupResult confirmPickup(Long orderId, Long adminId);

    MissedPickupRefundResult refundMissedPickup(Long orderId, Long adminId);
}
