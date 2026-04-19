package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

/**
 * 예약 제작 주문 관리 유스케이스.
 *
 * <p>예상 출고일 설정, 지연 요청, 제작 재개, 제작 완료를 지원한다.
 */
public interface OrderProductionUseCase {

    record ProductionResult(Long orderId, OrderStatus status, LocalDate expectedShipDate) {
        public static ProductionResult of(Order order, Fulfillment fulfillment) {
            return new ProductionResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
        }
    }

    ProductionResult setExpectedShipDate(Long orderId, LocalDate expectedShipDate);

    ProductionResult requestDelay(Long orderId);

    ProductionResult resumeProduction(Long orderId, Long adminId);

    ProductionResult completeProduction(Long orderId, Long adminId);
}
