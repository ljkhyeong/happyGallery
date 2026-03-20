package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.web.admin.dto.AdminOrderResponse;
import com.personal.happygallery.app.web.admin.dto.OrderHistoryResponse;
import com.personal.happygallery.domain.order.OrderStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultAdminOrderQueryService implements AdminOrderQueryUseCase {

    private final OrderReaderPort orderReaderPort;
    private final OrderHistoryPort orderHistoryPort;

    public DefaultAdminOrderQueryService(OrderReaderPort orderReaderPort,
                                   OrderHistoryPort orderHistoryPort) {
        this.orderReaderPort = orderReaderPort;
        this.orderHistoryPort = orderHistoryPort;
    }

    /** 관리자 주문 목록 조회 — 선택적 상태 필터, 최신 생성순 */
    public List<AdminOrderResponse> listOrders(OrderStatus status) {
        var orders = (status != null)
                ? orderReaderPort.findByStatusOrderByCreatedAtDesc(status)
                : orderReaderPort.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(AdminOrderResponse::from).toList();
    }

    /** 관리자 주문 결정 이력 조회 — 결정 시간순 */
    public List<OrderHistoryResponse> getOrderHistory(Long orderId) {
        return orderHistoryPort.findByOrderIdOrderByDecidedAtAsc(orderId).stream()
                .map(OrderHistoryResponse::from)
                .toList();
    }
}
