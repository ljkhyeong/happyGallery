package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.time.LocalDateTime;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAutoRefundProcessor {
    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final OrderApprovalService orderApprovalService;
    private final OrderHistoryPort orderHistoryPort;
    private final NotificationService notificationService;

    public OrderAutoRefundProcessor(OrderReaderPort orderReader,
                                    OrderStorePort orderStore,
                                    OrderApprovalService orderApprovalService,
                                    OrderHistoryPort orderHistoryPort,
                                    NotificationService notificationService) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderApprovalService = orderApprovalService;
        this.orderHistoryPort = orderHistoryPort;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public boolean process(Long orderId, LocalDateTime now) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        if (!order.canAutoRefund(now)) {
            return false;
        }

        orderApprovalService.restoreInventory(order);
        orderApprovalService.processRefund(order);
        order.markAutoRefunded();
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.AUTO_REFUND));
        orderStore.saveAndFlush(order);

        notificationService.notifyByGuestId(order.getGuestId(), NotificationEventType.ORDER_REFUNDED);
        return true;
    }
}
