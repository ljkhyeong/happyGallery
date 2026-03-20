package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupExpireProcessor {
    private final FulfillmentPort fulfillmentPort;
    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final DefaultOrderApprovalService orderApprovalService;

    public PickupExpireProcessor(FulfillmentPort fulfillmentPort,
                                 OrderReaderPort orderReader,
                                 OrderStorePort orderStore,
                                 DefaultOrderApprovalService orderApprovalService) {
        this.fulfillmentPort = fulfillmentPort;
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderApprovalService = orderApprovalService;
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
        if (order.getStatus() != OrderStatus.PICKUP_READY) {
            return false;
        }

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        if (fulfillment.getPickupDeadlineAt() == null || !fulfillment.getPickupDeadlineAt().isBefore(now)) {
            return false;
        }

        orderApprovalService.restoreInventory(order);
        boolean refundSucceeded = orderApprovalService.processRefund(order);
        order.markPickupExpired();

        orderStore.save(order);

        orderApprovalService.notifyRefundedGuest(order, refundSucceeded);
        return true;
    }
}
