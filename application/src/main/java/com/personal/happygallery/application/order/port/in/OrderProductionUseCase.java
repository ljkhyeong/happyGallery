package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

/** 주문 이행 일정과 주문제작 진행을 관리하는 유스케이스. */
public interface OrderProductionUseCase {

    record SetExpectedShipDateCommand(Long orderId, LocalDate expectedShipDate, Long adminId) {}

    record ProposeDelayCommand(Long orderId, Long adminId) {}

    record ProductionResult(Long orderId, OrderStatus status, LocalDate expectedShipDate) {
        public static ProductionResult of(Order order, Fulfillment fulfillment) {
            return new ProductionResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
        }
    }

    record DelayCancellationResult(ProductionResult production, Refund refund) {}

    ProductionResult setExpectedShipDate(SetExpectedShipDateCommand command);

    ProductionResult proposeDelay(ProposeDelayCommand command);

    DelayCancellationResult cancelForDelayRejection(Long orderId, Long adminId);

    ProductionResult resumeAfterDelay(Long orderId, Long adminId);

    ProductionResult completeProduction(Long orderId, Long adminId);
}
