package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.app.payment.RefundExecutionService;
import com.personal.happygallery.app.product.InventoryService;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 환불 공통 보조 로직.
 *
 * <p>재고 복구 → PG 환불 → 환불 알림 순서를 단일 지점에서 강제하여
 * 주문 거절·자동환불·픽업 만료에서 동일한 보상 흐름을 보장한다.
 */
@Service
class OrderRefundSupport {

    private final OrderItemPort orderItemPort;
    private final InventoryService inventoryService;
    private final RefundExecutionService refundExecutionService;
    private final ApplicationEventPublisher eventPublisher;

    OrderRefundSupport(OrderItemPort orderItemPort,
                       InventoryService inventoryService,
                       RefundExecutionService refundExecutionService,
                       ApplicationEventPublisher eventPublisher) {
        this.orderItemPort = orderItemPort;
        this.inventoryService = inventoryService;
        this.refundExecutionService = refundExecutionService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 재고 복구 → PG 환불 → 환불 알림을 순서대로 수행한다.
     *
     * <p>환불 성공 시에만 게스트에게 알림을 발송한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void refundOrder(Order order) {
        List<OrderItem> items = orderItemPort.findByOrder(order);
        for (OrderItem item : items) {
            inventoryService.restore(item.getProductId(), item.getQty());
        }

        Refund refund = refundExecutionService.processOrderRefund(order.getId(), order.getTotalAmount());
        if (refund.getStatus() == RefundStatus.SUCCEEDED) {
            eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(order.getGuestId(), NotificationEventType.ORDER_REFUNDED));
        }
    }
}
