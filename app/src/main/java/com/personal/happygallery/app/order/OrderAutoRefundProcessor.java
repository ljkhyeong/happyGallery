package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAutoRefundProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoRefundProcessor.class);

    private final OrderRepository orderRepository;
    private final OrderApprovalService orderApprovalService;
    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    private final NotificationService notificationService;

    public OrderAutoRefundProcessor(OrderRepository orderRepository,
                                    OrderApprovalService orderApprovalService,
                                    OrderApprovalHistoryRepository orderApprovalHistoryRepository,
                                    NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderApprovalService = orderApprovalService;
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        if (!order.canAutoRefund(now)) {
            return false;
        }

        orderApprovalService.restoreInventory(order);
        orderApprovalService.processRefund(order);
        order.markAutoRefunded();
        orderApprovalHistoryRepository.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.AUTO_REFUND));
        orderRepository.saveAndFlush(order);

        notificationService.notifyByGuestId(order.getGuestId(), NotificationEventType.ORDER_REFUNDED);
        return true;
    }
}
