package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Order;
import java.util.List;

/**
 * 주문 생성 유스케이스.
 *
 * <p>현재 회원 장바구니 checkout 경로에서 사용한다.
 */
public interface OrderCreationUseCase {

    record OrderItemInput(Long productId, int qty) {}

    Order createMemberOrder(Long userId, List<OrderItemInput> items);
}
