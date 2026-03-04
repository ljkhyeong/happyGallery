package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.product.InventoryService;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.payment.PaymentProvider;
import com.personal.happygallery.infra.payment.RefundResult;
import com.personal.happygallery.infra.product.ProductRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 승인/거절 서비스.
 *
 * <ul>
 *   <li>{@link #approve(Long)} — 관리자 승인 → {@link com.personal.happygallery.domain.order.OrderStatus#APPROVED_FULFILLMENT_PENDING}</li>
 *   <li>{@link #reject(Long)} — 관리자 거절 → 재고 복구 → PG 환불 → {@link com.personal.happygallery.domain.order.OrderStatus#REJECTED_REFUNDED}</li>
 * </ul>
 *
 * <p>이미 환불된 주문에 대한 승인/거절 시도는
 * {@link com.personal.happygallery.common.error.AlreadyRefundedException}(409)을 던진다.
 */
@Service
@Transactional
public class OrderApprovalService {

    private static final Logger log = LoggerFactory.getLogger(OrderApprovalService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final RefundRepository refundRepository;
    private final PaymentProvider paymentProvider;
    private final ProductRepository productRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    private final NotificationService notificationService;

    public OrderApprovalService(OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                InventoryService inventoryService,
                                RefundRepository refundRepository,
                                PaymentProvider paymentProvider,
                                ProductRepository productRepository,
                                FulfillmentRepository fulfillmentRepository,
                                OrderApprovalHistoryRepository orderApprovalHistoryRepository,
                                NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.refundRepository = refundRepository;
        this.paymentProvider = paymentProvider;
        this.productRepository = productRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
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
    public Order approve(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));

        boolean isMadeToOrder = isMadeToOrderOrder(order);
        if (isMadeToOrder) {
            order.approveAsProduction();
            fulfillmentRepository.save(
                    new Fulfillment(order.getId(), FulfillmentType.SHIPPING, OrderStatus.IN_PRODUCTION));
        } else {
            order.approve();
        }
        orderApprovalHistoryRepository.save(new OrderApprovalHistory(order.getId(), OrderApprovalDecision.APPROVE));
        return orderRepository.save(order);
    }

    private boolean isMadeToOrderOrder(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("상품"));
            if (product.getType() == ProductType.MADE_TO_ORDER) {
                return true;
            }
        }
        return false;
    }

    /**
     * 주문을 거절한다.
     *
     * <ol>
     *   <li>재고 복구</li>
     *   <li>환불 요청 기록 + PG 환불 호출</li>
     *   <li>주문 상태 → {@link com.personal.happygallery.domain.order.OrderStatus#REJECTED_REFUNDED}</li>
     * </ol>
     *
     * @param orderId 주문 ID
     * @return 거절된 주문
     */
    public Order reject(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.reject();

        restoreInventory(order);
        processRefund(order);
        orderApprovalHistoryRepository.save(new OrderApprovalHistory(order.getId(), OrderApprovalDecision.REJECT));
        notificationService.notifyByGuestId(order.getGuestId(), NotificationEventType.ORDER_REFUNDED);

        return orderRepository.save(order);
    }

    void restoreInventory(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            inventoryService.restore(item.getProductId(), item.getQty());
        }
    }

    void processRefund(Order order) {
        Refund refund = refundRepository.save(new Refund(order.getId(), order.getTotalAmount()));
        try {
            RefundResult result = paymentProvider.refund(null, order.getTotalAmount());
            if (result.success()) {
                refund.markSucceeded(result.pgRef());
            } else {
                log.warn("환불 실패 [orderId={}, refundId={}] reason={}",
                        order.getId(), refund.getId(), result.failReason());
                refund.markFailed(result.failReason());
            }
        } catch (Exception e) {
            log.error("환불 호출 예외 [orderId={}, refundId={}]", order.getId(), refund.getId(), e);
            refund.markFailed(e.getMessage());
        }
        refundRepository.save(refund);
    }
}
