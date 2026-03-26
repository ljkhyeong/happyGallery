package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.OptimisticLockRetryable;
import com.personal.happygallery.common.error.NotFoundException;
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
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
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
