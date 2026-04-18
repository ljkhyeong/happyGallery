package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.order.OrderStatus;
import java.util.List;

/**
 * 관리자 주문 조회 유스케이스.
 *
 * <p>상태별 목록 조회와 결정 이력 조회를 지원한다.
 */
public interface AdminOrderQueryUseCase {

    List<AdminOrderResponse> listOrders(OrderStatus status);

    CursorPage<AdminOrderResponse> listOrders(OrderStatus status, String cursor, int size);

    List<OrderHistoryResponse> getOrderHistory(Long orderId);
}
