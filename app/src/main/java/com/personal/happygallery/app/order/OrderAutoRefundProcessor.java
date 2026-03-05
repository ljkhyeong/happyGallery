package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.LocalDateTime;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAutoRefundProcessor {

    private final OrderRepository orderRepository;
    private final OrderApprovalService orderApprovalService;
    private final NotificationService notificationService;

    public OrderAutoRefundProcessor(OrderRepository orderRepository,
                                    OrderApprovalService orderApprovalService,
                                    NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderApprovalService = orderApprovalService;
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
        if (order.getStatus() != OrderStatus.PAID_APPROVAL_PENDING) {
            return false;
        }
        if (order.getApprovalDeadlineAt() == null || !order.getApprovalDeadlineAt().isBefore(now)) {
            return false;
        }

        orderApprovalService.restoreInventory(order);
        orderApprovalService.processRefund(order);
        order.markAutoRefunded();
        orderRepository.saveAndFlush(order);
        notificationService.notifyByGuestId(order.getGuestId(), NotificationEventType.ORDER_REFUNDED);
        return true;
    }
}
