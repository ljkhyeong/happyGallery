package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.app.order.OrderQueryService.OrderDetail;
import com.personal.happygallery.domain.order.Order;
import java.util.List;

/**
 * 주문 조회 유스케이스.
 *
 * <p>토큰 기반(비회원) / 회원 두 경로를 지원한다.
 */
public interface OrderQueryUseCase {

    List<Order> listMyOrders(Long userId);

    OrderDetail findMyOrder(Long id, Long userId);

    OrderDetail getOrderByToken(Long orderId, String rawToken);
}
