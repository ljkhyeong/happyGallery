package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.web.CursorPage;
import com.personal.happygallery.app.web.CursorUtils;
import com.personal.happygallery.app.web.admin.dto.AdminOrderResponse;
import com.personal.happygallery.app.web.admin.dto.OrderHistoryResponse;
import com.personal.happygallery.domain.order.Order;
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

    /** 관리자 주문 목록 조회 — 선택적 상태 필터, 최신 생성순 (레거시 전체 조회) */
    public List<AdminOrderResponse> listOrders(OrderStatus status) {
        var orders = (status != null)
                ? orderReaderPort.findByStatusOrderByCreatedAtDesc(status)
                : orderReaderPort.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(AdminOrderResponse::from).toList();
    }

    /** 관리자 주문 목록 조회 — 커서 기반 페이지네이션 */
    public CursorPage<AdminOrderResponse> listOrders(OrderStatus status, String cursor, int size) {
        int fetchSize = size + 1;
        List<Order> orders;

        if (cursor == null) {
            orders = (status != null)
                    ? orderReaderPort.findByStatusOrderByCreatedAtDesc(status, fetchSize)
                    : orderReaderPort.findAllOrderByCreatedAtDesc(fetchSize);
        } else {
            var param = CursorUtils.decode(cursor);
            orders = (status != null)
                    ? orderReaderPort.findByStatusOrderByCreatedAtDescAfterCursor(
                            status, param.timestamp(), param.id(), fetchSize)
                    : orderReaderPort.findAllOrderByCreatedAtDescAfterCursor(
                            param.timestamp(), param.id(), fetchSize);
        }

        List<AdminOrderResponse> items = orders.stream()
                .map(AdminOrderResponse::from)
                .toList();

        return CursorPage.of(items, size,
                r -> CursorUtils.encode(r.createdAt(), r.orderId()));
    }

    /** 관리자 주문 결정 이력 조회 — 결정 시간순 */
    public List<OrderHistoryResponse> getOrderHistory(Long orderId) {
        return orderHistoryPort.findByOrderIdOrderByDecidedAtAsc(orderId).stream()
                .map(OrderHistoryResponse::from)
                .toList();
    }
}
