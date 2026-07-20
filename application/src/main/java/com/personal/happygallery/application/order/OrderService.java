package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.InventoryService.InventoryAdjustment;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성 서비스.
 *
 * <p>결제 완료 시 호출. 재고를 차감하고 주문을 {@link com.personal.happygallery.domain.order.OrderStatus#PAID_APPROVAL_PENDING}
 * 상태로 생성한다. 승인 마감은 결제 시각 + 24시간.
 */
@Service
@Transactional
public class OrderService {

    private final OrderStorePort orderStore;
    private final OrderItemPort orderItemPort;
    private final FulfillmentPort fulfillmentPort;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final GuestTokenService guestTokenService;
    private final ShippingAddressProtector shippingAddressProtector;
    private final Clock clock;

    public OrderService(OrderStorePort orderStore,
                        OrderItemPort orderItemPort,
                        FulfillmentPort fulfillmentPort,
                        InventoryService inventoryService,
                        ApplicationEventPublisher eventPublisher,
                        GuestTokenService guestTokenService,
                        ShippingAddressProtector shippingAddressProtector,
                        Clock clock) {
        this.orderStore = orderStore;
        this.orderItemPort = orderItemPort;
        this.fulfillmentPort = fulfillmentPort;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
        this.guestTokenService = guestTokenService;
        this.shippingAddressProtector = shippingAddressProtector;
        this.clock = clock;
    }

    /**
     * 결제 완료 주문을 생성한다.
     *
     * <ol>
     *   <li>각 상품의 재고를 차감한다 (재고 부족 시 {@link com.personal.happygallery.domain.error.InventoryNotEnoughException}).</li>
     *   <li>주문을 {@link com.personal.happygallery.domain.order.OrderStatus#PAID_APPROVAL_PENDING}으로 저장한다.</li>
     *   <li>승인 마감({@code approvalDeadlineAt})을 결제 시각 + 24시간으로 설정한다.</li>
     * </ol>
     *
     * @param guestId 비회원 ID (회원 주문은 null)
     * @param items   주문 상품 목록
     * @return 생성된 주문
     */
    public OrderCreationResult createPaidOrder(Long guestId, List<OrderItemRequest> items,
                                               FulfillmentType fulfillmentType,
                                               ShippingAddress shippingAddress) {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        long totalAmount = totalAmount(items);

        GuestTokenService.IssuedToken issued = guestTokenService.issue();
        String rawToken = issued.rawToken();
        String tokenHash = issued.tokenHash();
        Order order = orderStore.save(
                Order.forGuest(guestId, tokenHash, totalAmount, paidAt, paidAt.plusHours(24)));

        saveItemsAndDeductInventory(order, items);
        saveFulfillment(order, fulfillmentType, shippingAddress);

        eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                guestId,
                NotificationEventType.ORDER_PAID,
                "ORDER",
                order.getId()));

        return new OrderCreationResult(order, rawToken);
    }

    /**
     * 회원 주문 생성. guest 대신 user_id를 설정한다. accessToken 없음.
     */
    public Order createMemberOrder(Long userId, List<OrderItemRequest> items,
                                   FulfillmentType fulfillmentType,
                                   ShippingAddress shippingAddress) {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        long totalAmount = totalAmount(items);

        Order order = orderStore.save(
                Order.forMember(userId, totalAmount, paidAt, paidAt.plusHours(24)));

        saveItemsAndDeductInventory(order, items);
        saveFulfillment(order, fulfillmentType, shippingAddress);

        eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                userId,
                NotificationEventType.ORDER_PAID,
                "ORDER",
                order.getId()));

        return order;
    }

    /** 테스트·내부 fixture용 기본 픽업 주문 생성 경로. 운영 결제는 수령 방법을 명시한다. */
    public OrderCreationResult createPaidOrder(Long guestId, List<OrderItemRequest> items) {
        return createPaidOrder(guestId, items, FulfillmentType.PICKUP, null);
    }

    /** 테스트·내부 fixture용 기본 픽업 주문 생성 경로. 운영 결제는 수령 방법을 명시한다. */
    public Order createMemberOrder(Long userId, List<OrderItemRequest> items) {
        return createMemberOrder(userId, items, FulfillmentType.PICKUP, null);
    }

    private void saveItemsAndDeductInventory(Order order, List<OrderItemRequest> items) {
        inventoryService.deductAll(items.stream()
                .map(item -> new InventoryAdjustment(item.productId(), item.qty()))
                .toList());
        items.forEach(item -> orderItemPort.save(
                new OrderItem(order, item.productId(), item.qty(), item.unitPrice())));
    }

    private static long totalAmount(List<OrderItemRequest> items) {
        long total = 0L;
        for (OrderItemRequest item : items) {
            total = OrderAmountCalculator.addLine(total, item.qty(), item.unitPrice());
        }
        return total;
    }

    private void saveFulfillment(Order order,
                                 FulfillmentType fulfillmentType,
                                 ShippingAddress shippingAddress) {
        if (fulfillmentType == null) {
            throw new IllegalArgumentException("fulfillmentType must not be null");
        }
        Fulfillment fulfillment = switch (fulfillmentType) {
            case PICKUP -> Fulfillment.pickup(order.getId());
            case SHIPPING -> Fulfillment.shipping(
                    order.getId(), shippingAddressProtector.encrypt(shippingAddress));
        };
        fulfillmentPort.save(fulfillment);
    }

    public record OrderItemRequest(Long productId, int qty, long unitPrice) {}

    public record OrderCreationResult(Order order, String rawAccessToken) {}
}
