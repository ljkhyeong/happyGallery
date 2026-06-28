package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.order.Order;
import java.util.List;

/**
 * Cart checkout 흐름이 상품 가격 확정과 주문 생성 세부 구현을 직접 알지 않도록 분리한 내부 협력 서비스.
 */
public interface OrderCreationService {

    record OrderItemInput(Long productId, int qty) {}

    Order createMemberOrder(Long userId, List<OrderItemInput> items);
}
