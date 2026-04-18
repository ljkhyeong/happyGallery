package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
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

    public PickupExpireProcessor(FulfillmentPort fulfillmentPort,
                                 OrderReaderPort orderReader,
                                 OrderStorePort orderStore,
                                 OrderRefundSupport orderRefundSupport) {
        this.fulfillmentPort = fulfillmentPort;
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderRefundSupport = orderRefundSupport;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @OptimisticLockRetryable
    public boolean process(Long orderId, LocalDateTime now) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
        if (order.getStatus() != OrderStatus.PICKUP_READY) {
            return false;
        }

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));
        if (fulfillment.getPickupDeadlineAt() == null || !fulfillment.getPickupDeadlineAt().isBefore(now)) {
            return false;
        }

        orderRefundSupport.refundOrder(order);
        order.markPickupExpired();
        orderStore.save(order);
        return true;
    }
}
