package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.app.payment.RefundExecutionService;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.app.product.InventoryService;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 승인/거절 서비스.
 *
 * <ul>
 *   <li>{@link #approve(Long)} — 관리자 승인 → {@link com.personal.happygallery.domain.order.OrderStatus#APPROVED_FULFILLMENT_PENDING}</li>
 *   <li>{@link #reject(Long)} — 관리자 거절 → 재고 복구 → PG 환불 → {@link com.personal.happygallery.domain.order.OrderStatus#REJECTED}</li>
 * </ul>
 *
 * <p>이미 환불된 주문에 대한 승인/거절 시도는
 * {@link com.personal.happygallery.common.error.AlreadyRefundedException}(409)을 던진다.
 */
@Service
@Transactional
public class OrderApprovalService implements OrderApprovalUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderApprovalService.class);

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final OrderItemPort orderItemPort;
    private final InventoryService inventoryService;
    private final RefundExecutionService refundExecutionService;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;
    private final NotificationService notificationService;

    public OrderApprovalService(OrderReaderPort orderReader,
                                OrderStorePort orderStore,
                                OrderItemPort orderItemPort,
                                InventoryService inventoryService,
                                RefundExecutionService refundExecutionService,
                                FulfillmentPort fulfillmentPort,
                                OrderHistoryPort orderHistoryPort,
                                NotificationService notificationService) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderItemPort = orderItemPort;
        this.inventoryService = inventoryService;
        this.refundExecutionService = refundExecutionService;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
        this.notificationService = notificationService;
    }

    /**
     * 주문을 승인한다. 이미 환불된 주문은 409.
     *
     * <p>주문 내 상품 중 {@link ProductType#MADE_TO_ORDER}가 하나라도 있으면
     * {@link Order#approveAsProduction()}을 호출하여 {@link OrderStatus#IN_PRODUCTION}으로 전이하고
     * Fulfillment 레코드를 생성한다. 그 외에는 {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}으로 전이한다.
     *
     * @param orderId 주문 ID
     * @return 승인된 주문
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, random = true))
    public Order approve(Long orderId) {
        return approve(orderId, null);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public Order approve(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));

        boolean isMadeToOrder = isMadeToOrderOrder(order);
        if (isMadeToOrder) {
            order.approveAsProduction();
            fulfillmentPort.save(
                    new Fulfillment(order.getId(), FulfillmentType.SHIPPING));
        } else {
            order.approve();
        }
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.APPROVE, adminId, null));
        log.info("order approved [orderId={} adminId={} madeToOrder={}]", orderId, adminId, isMadeToOrder);
        return orderStore.save(order);
    }

    private boolean isMadeToOrderOrder(Order order) {
        return orderItemPort.existsByOrderAndProductType(order, ProductType.MADE_TO_ORDER);
    }

    /**
     * 주문을 거절한다.
     *
     * <ol>
     *   <li>재고 복구</li>
     *   <li>환불 요청 기록 + PG 환불 호출</li>
     *   <li>주문 상태 → {@link com.personal.happygallery.domain.order.OrderStatus#REJECTED}</li>
     * </ol>
     *
     * @param orderId 주문 ID
     * @return 거절된 주문
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, random = true))
    public Order reject(Long orderId) {
        return reject(orderId, null);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public Order reject(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.reject();

        restoreInventory(order);
        processRefund(order);
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.REJECT, adminId, null));
        notificationService.notifyByGuestId(order.getGuestId(), NotificationEventType.ORDER_REFUNDED);
        log.info("order rejected [orderId={} adminId={}]", orderId, adminId);

        return orderStore.save(order);
    }

    void restoreInventory(Order order) {
        List<OrderItem> items = orderItemPort.findByOrder(order);
        for (OrderItem item : items) {
            inventoryService.restore(item.getProductId(), item.getQty());
        }
    }

    void processRefund(Order order) {
        refundExecutionService.processOrderRefund(order.getId(), order.getTotalAmount());
    }
}
