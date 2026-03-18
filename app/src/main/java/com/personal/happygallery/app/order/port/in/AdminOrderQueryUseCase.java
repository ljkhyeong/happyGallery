package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.app.web.admin.dto.AdminOrderResponse;
import com.personal.happygallery.app.web.admin.dto.OrderHistoryResponse;
import com.personal.happygallery.domain.order.OrderStatus;
import java.util.List;

/**
 * 관리자 주문 조회 유스케이스.
 *
 * <p>상태별 목록 조회와 결정 이력 조회를 지원한다.
 */
public interface AdminOrderQueryUseCase {

    List<AdminOrderResponse> listOrders(OrderStatus status);

    List<OrderHistoryResponse> getOrderHistory(Long orderId);
}
