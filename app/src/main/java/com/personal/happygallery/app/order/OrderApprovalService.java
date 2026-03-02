package com.personal.happygallery.app.order;

import com.personal.happygallery.app.product.InventoryService;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.payment.PaymentProvider;
import com.personal.happygallery.infra.payment.RefundResult;
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

    public OrderApprovalService(OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                InventoryService inventoryService,
                                RefundRepository refundRepository,
                                PaymentProvider paymentProvider) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.refundRepository = refundRepository;
        this.paymentProvider = paymentProvider;
    }

    /**
     * 주문을 승인한다. 이미 환불된 주문은 409.
     *
     * @param orderId 주문 ID
     * @return 승인된 주문
     */
    public Order approve(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.approve();
        return orderRepository.save(order);
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.reject();

        restoreInventory(order);
        processRefund(order);

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
