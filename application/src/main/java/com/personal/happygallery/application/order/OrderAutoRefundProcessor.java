package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAutoRefundProcessor {
    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final OrderRefundSupport orderRefundSupport;
    private final OrderHistoryPort orderHistoryPort;

    public OrderAutoRefundProcessor(OrderReaderPort orderReader,
                                    OrderStorePort orderStore,
                                    OrderRefundSupport orderRefundSupport,
                                    OrderHistoryPort orderHistoryPort) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderRefundSupport = orderRefundSupport;
        this.orderHistoryPort = orderHistoryPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @OptimisticLockRetryable
    public boolean process(Long orderId, LocalDateTime now) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        if (!order.canAutoRefund(now)) {
            return false;
        }

        orderRefundSupport.refundOrder(order);
        order.markAutoRefunded();
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.AUTO_REFUND));
        orderStore.saveAndFlush(order);
        return true;
    }
}
