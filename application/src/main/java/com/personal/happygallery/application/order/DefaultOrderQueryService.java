package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingEventPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.payment.PaymentReceiptQuery;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.application.customer.port.out.MemberHistoryReaderPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.application.token.GuestTokenService;
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
    private final RefundPort refundPort;
    private final ShippingAddressProtector shippingAddressProtector;
    private final ShipmentTrackingEventPort trackingEventPort;
    private final PaymentReceiptQuery receiptQuery;
    private final MemberHistoryReaderPort memberHistoryReader;

    public DefaultOrderQueryService(OrderReaderPort orderReader,
                                    OrderItemPort orderItemPort,
                                    FulfillmentPort fulfillmentPort,
                                    GuestTokenService guestTokenService,
                                    RefundPort refundPort,
                                    ShippingAddressProtector shippingAddressProtector,
                                    ShipmentTrackingEventPort trackingEventPort,
                                    PaymentReceiptQuery receiptQuery,
                                    MemberHistoryReaderPort memberHistoryReader) {
        this.orderReader = orderReader;
        this.orderItemPort = orderItemPort;
        this.fulfillmentPort = fulfillmentPort;
        this.guestTokenService = guestTokenService;
        this.refundPort = refundPort;
        this.shippingAddressProtector = shippingAddressProtector;
        this.trackingEventPort = trackingEventPort;
        this.receiptQuery = receiptQuery;
        this.memberHistoryReader = memberHistoryReader;
    }

    /** 회원 — 자기 주문 목록 조회 */
    @Override
    public List<Order> listMyOrders(Long userId) {
        return listMyOrders(userId, null, PageParams.MAX_SIZE).content();
    }

    @Override
    public CursorPage<Order> listMyOrders(Long userId, OrderHistoryQuery query, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        if (query.isDefault()) return listMyOrders(userId, cursor, pageSize);
        return memberHistoryReader.findOrders(userId, query, cursor, pageSize);
    }

    @Override
    public CursorPage<Order> listMyOrders(Long userId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        List<Order> orders;
        if (cursor == null) {
            orders = orderReader.findByUserIdOrderByCreatedAtDesc(userId, fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            orders = orderReader.findByUserIdOrderByCreatedAtDescAfterCursor(
                    userId, cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        return CursorPage.of(
                orders,
                pageSize,
                order -> CursorUtils.encode(order.getCreatedAt(), order.getId()));
    }

    /** 회원 — 자기 주문 상세 조회 (소유권 검증 포함) */
    @Override
    public OrderDetail findMyOrder(Long id, Long userId) {
        Order order = orderReader.findById(id)
                .filter(o -> Objects.equals(o.getUserId(), userId))
                .orElseThrow(NotFoundException.supplier("주문"));
        List<OrderItem> items = orderItemPort.findByOrder(order);
        Fulfillment fulfillment = fulfillmentPort.findByOrderId(id).orElse(null);
        return detail(order, items, fulfillment);
    }

    /** 토큰 기반 주문 상세 조회 — 입력 토큰을 SHA-256 해시 후 비교 */
    @Override
    public OrderDetail getOrderByToken(Long orderId, String rawToken) {
        String tokenHash = guestTokenService.resolveTokenHash(rawToken);
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        if (!Objects.equals(order.getAccessToken(), tokenHash)) {
            throw new NotFoundException("주문");
        }
        List<OrderItem> items = orderItemPort.findByOrder(order);
        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId).orElse(null);
        return detail(order, items, fulfillment);
    }

    private OrderDetail detail(Order order, List<OrderItem> items, Fulfillment fulfillment) {
        ShippingAddress shippingAddress = fulfillment != null
                && fulfillment.getType() == FulfillmentType.SHIPPING
                && fulfillment.getShippingAddressEnc() != null
                ? shippingAddressProtector.decrypt(fulfillment.getShippingAddressEnc())
                : null;
        return new OrderDetail(
                order,
                items,
                fulfillment,
                shippingAddress,
                trackingEventPort.findByOrderIdOrderByOccurredAtAsc(order.getId()),
                refundPort.findDirectByOrderId(order.getId()).orElse(null),
                receiptQuery.findReceipt(PaymentContext.ORDER, order.getId()));
    }
}
