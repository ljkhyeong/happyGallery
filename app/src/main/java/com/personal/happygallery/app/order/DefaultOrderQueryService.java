package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.app.token.GuestTokenService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultOrderQueryService implements OrderQueryUseCase {

    private final OrderReaderPort orderReader;
    private final OrderItemPort orderItemPort;
    private final FulfillmentPort fulfillmentPort;
    private final GuestTokenService guestTokenService;

    public DefaultOrderQueryService(OrderReaderPort orderReader,
                                    OrderItemPort orderItemPort,
                                    FulfillmentPort fulfillmentPort,
                                    GuestTokenService guestTokenService) {
        this.orderReader = orderReader;
        this.orderItemPort = orderItemPort;
        this.fulfillmentPort = fulfillmentPort;
        this.guestTokenService = guestTokenService;
    }

    /** 회원 — 자기 주문 목록 조회 */
    public List<Order> listMyOrders(Long userId) {
        return orderReader.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** 회원 — 자기 주문 상세 조회 (소유권 검증 포함) */
    public OrderDetail findMyOrder(Long id, Long userId) {
        Order order = orderReader.findById(id)
                .filter(o -> Objects.equals(o.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("주문"));
        List<OrderItem> items = orderItemPort.findByOrder(order);
        Fulfillment fulfillment = fulfillmentPort.findByOrderId(id).orElse(null);
        return new OrderDetail(order, items, fulfillment);
    }

    /** 토큰 기반 주문 상세 조회 — 입력 토큰을 SHA-256 해시 후 비교 */
    public OrderDetail getOrderByToken(Long orderId, String rawToken) {
        String tokenHash = guestTokenService.resolveTokenHash(rawToken);
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        if (!Objects.equals(order.getAccessToken(), tokenHash)) {
            throw new NotFoundException("주문");
        }
        List<OrderItem> items = orderItemPort.findByOrder(order);
        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId).orElse(null);
        return new OrderDetail(order, items, fulfillment);
    }
}
