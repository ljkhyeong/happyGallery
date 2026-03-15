package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FulfillmentRepository fulfillmentRepository;

    public OrderQueryService(OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             FulfillmentRepository fulfillmentRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    public record OrderDetail(Order order, List<OrderItem> items, Fulfillment fulfillment) {}

    /** 회원 — 자기 주문 목록 조회 */
    public List<Order> listMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** 회원 — 자기 주문 상세 조회 (소유권 검증 포함) */
    public OrderDetail findMyOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .filter(o -> Objects.equals(o.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("주문"));
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(id).orElse(null);
        return new OrderDetail(order, items, fulfillment);
    }

    /** 토큰 기반 주문 상세 조회 */
    public OrderDetail getOrderByToken(Long orderId, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        if (!Objects.equals(order.getAccessToken(), token)) {
            throw new NotFoundException("주문");
        }
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(orderId).orElse(null);
        return new OrderDetail(order, items, fulfillment);
    }
}
