package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupExpireProcessor {
    private final FulfillmentPort fulfillmentPort;
    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final OrderRefundSupport orderRefundSupport;
    private final OrderHistoryPort orderHistoryPort;

    public PickupExpireProcessor(FulfillmentPort fulfillmentPort,
                                 OrderReaderPort orderReader,
                                 OrderStorePort orderStore,
                                 OrderRefundSupport orderRefundSupport,
                                 OrderHistoryPort orderHistoryPort) {
        this.fulfillmentPort = fulfillmentPort;
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderRefundSupport = orderRefundSupport;
        this.orderHistoryPort = orderHistoryPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @OptimisticLockRetryable
    public boolean process(Long orderId, LocalDateTime now) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        if (order.getStatus() != OrderStatus.PICKUP_READY) {
            return false;
        }

        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        if (fulfillment.getPickupDeadlineAt() == null || !fulfillment.getPickupDeadlineAt().isBefore(now)) {
            return false;
        }

        orderRefundSupport.refundOrder(order);
        order.markPickupExpired();
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PICKUP_EXPIRED));
        orderStore.save(order);
        return true;
    }
}
