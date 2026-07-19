package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.application.order.port.in.AdminOrderFulfillmentResponse;
import com.personal.happygallery.application.order.port.in.OrderHistoryResponse;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
@Transactional(readOnly = true)
public class DefaultAdminOrderQueryService implements AdminOrderQueryUseCase {

    private final OrderReaderPort orderReaderPort;
    private final OrderHistoryPort orderHistoryPort;
    private final FulfillmentPort fulfillmentPort;
    private final ShippingAddressProtector shippingAddressProtector;

    public DefaultAdminOrderQueryService(OrderReaderPort orderReaderPort,
                                         OrderHistoryPort orderHistoryPort,
                                         FulfillmentPort fulfillmentPort,
                                         ShippingAddressProtector shippingAddressProtector) {
        this.orderReaderPort = orderReaderPort;
        this.orderHistoryPort = orderHistoryPort;
        this.fulfillmentPort = fulfillmentPort;
        this.shippingAddressProtector = shippingAddressProtector;
    }

    /** 관리자 주문 목록 조회 — 선택적 상태 필터, 최신 생성순 (레거시 전체 조회) */
    @Override
    public List<AdminOrderResponse> listOrders(OrderStatus status) {
        var orders = (status != null)
                ? orderReaderPort.findByStatusOrderByCreatedAtDesc(status)
                : orderReaderPort.findAllByOrderByCreatedAtDesc();
        return toResponses(orders);
    }

    /** 관리자 주문 목록 조회 — 커서 기반 페이지네이션 */
    @Override
    public CursorPage<AdminOrderResponse> listOrders(OrderStatus status, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
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

        List<AdminOrderResponse> items = toResponses(orders);

        return CursorPage.of(items, pageSize,
                r -> CursorUtils.encode(r.createdAt().toLocalDateTime(), r.orderId()));
    }

    @Override
    public AdminOrderFulfillmentResponse getFulfillment(Long orderId) {
        OrderLookups.requireOrder(orderReaderPort, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        var shippingAddress = fulfillment.getType() == FulfillmentType.SHIPPING
                && fulfillment.getShippingAddressEnc() != null
                ? shippingAddressProtector.decrypt(fulfillment.getShippingAddressEnc())
                : null;
        return AdminOrderFulfillmentResponse.from(fulfillment, shippingAddress);
    }

    /** 관리자 주문 처리 이력 조회 — 처리 시간순 */
    @Override
    public List<OrderHistoryResponse> getOrderHistory(Long orderId) {
        return orderHistoryPort.findByOrderIdOrderByDecidedAtAsc(orderId).stream()
                .map(OrderHistoryResponse::from)
                .toList();
    }

    private List<AdminOrderResponse> toResponses(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, Fulfillment> fulfillmentsByOrderId = fulfillmentPort.findByOrderIdIn(
                        orders.stream().map(Order::getId).toList())
                .stream()
                .collect(toMap(Fulfillment::getOrderId, Function.identity()));
        return orders.stream()
                .map(order -> AdminOrderResponse.from(order, fulfillmentsByOrderId.get(order.getId())))
                .toList();
    }
}
