package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.app.product.InventoryService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final Clock clock;

    public OrderService(OrderStorePort orderStore,
                        OrderItemPort orderItemPort,
                        InventoryService inventoryService,
                        NotificationService notificationService,
                        Clock clock) {
        this.orderStore = orderStore;
        this.orderItemPort = orderItemPort;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * 결제 완료 주문을 생성한다.
     *
     * <ol>
     *   <li>각 상품의 재고를 차감한다 (재고 부족 시 {@link com.personal.happygallery.common.error.InventoryNotEnoughException}).</li>
     *   <li>주문을 {@link com.personal.happygallery.domain.order.OrderStatus#PAID_APPROVAL_PENDING}으로 저장한다.</li>
     *   <li>승인 마감({@code approvalDeadlineAt})을 결제 시각 + 24시간으로 설정한다.</li>
     * </ol>
     *
     * @param guestId 비회원 ID (회원 주문은 null)
     * @param items   주문 상품 목록
     * @return 생성된 주문
     */
    public Order createPaidOrder(Long guestId, List<OrderItemRequest> items) {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        long totalAmount = items.stream().mapToLong(i -> (long) i.qty() * i.unitPrice()).sum();

        String accessToken = UUID.randomUUID().toString();
        Order order = orderStore.save(
                new Order(guestId, accessToken, totalAmount, paidAt, paidAt.plusHours(24)));

        for (OrderItemRequest item : items) {
            orderItemPort.save(new OrderItem(order, item.productId(), item.qty(), item.unitPrice()));
            inventoryService.deduct(item.productId(), item.qty());
        }

        notificationService.notifyByGuestId(guestId, NotificationEventType.ORDER_PAID);

        return order;
    }

    /**
     * 회원 주문 생성. guest 대신 user_id를 설정한다. accessToken 없음.
     */
    public Order createMemberOrder(Long userId, List<OrderItemRequest> items) {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        long totalAmount = items.stream().mapToLong(i -> (long) i.qty() * i.unitPrice()).sum();

        Order order = orderStore.save(
                Order.forMember(userId, totalAmount, paidAt, paidAt.plusHours(24)));

        for (OrderItemRequest item : items) {
            orderItemPort.save(new OrderItem(order, item.productId(), item.qty(), item.unitPrice()));
            inventoryService.deduct(item.productId(), item.qty());
        }

        return order;
    }

    public record OrderItemRequest(Long productId, int qty, long unitPrice) {}
}
