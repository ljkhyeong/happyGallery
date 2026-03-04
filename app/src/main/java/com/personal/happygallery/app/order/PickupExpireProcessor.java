package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.LocalDateTime;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupExpireProcessor {

    private final FulfillmentRepository fulfillmentRepository;
    private final OrderRepository orderRepository;
    private final OrderApprovalService orderApprovalService;

    public PickupExpireProcessor(FulfillmentRepository fulfillmentRepository,
                                 OrderRepository orderRepository,
                                 OrderApprovalService orderApprovalService) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderRepository = orderRepository;
        this.orderApprovalService = orderApprovalService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, random = true))
    public boolean process(Long orderId, LocalDateTime now) {
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        if (fulfillment.getStatus() != OrderStatus.PICKUP_READY) {
            return false;
        }
        if (fulfillment.getPickupDeadlineAt() == null || !fulfillment.getPickupDeadlineAt().isBefore(now)) {
            return false;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        if (order.getStatus() != OrderStatus.PICKUP_READY) {
            return false;
        }

        orderApprovalService.restoreInventory(order);
        orderApprovalService.processRefund(order);
        order.markPickupExpired();
        fulfillment.syncStatus(order.getStatus());

        orderRepository.save(order);
        fulfillmentRepository.saveAndFlush(fulfillment);
        return true;
    }
}
