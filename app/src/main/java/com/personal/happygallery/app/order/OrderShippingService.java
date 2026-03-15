package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배송 이행 관리 서비스.
 *
 * <p>배송 경로: {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}
 * → {@link OrderStatus#SHIPPING_PREPARING} → {@link OrderStatus#SHIPPED}
 * → {@link OrderStatus#DELIVERED}.
 */
@Service
@Transactional
public class OrderShippingService {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;

    public OrderShippingService(OrderReaderPort orderReader,
                                OrderStorePort orderStore,
                                FulfillmentPort fulfillmentPort,
                                OrderHistoryPort orderHistoryPort) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
    }

    /**
     * 배송 준비 시작. APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING.
     * Fulfillment가 SHIPPING 타입이어야 한다 (없으면 새로 생성).
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public ShippingResult prepareShipping(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.markShippingPreparing();

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseGet(() -> new Fulfillment(orderId, FulfillmentType.SHIPPING));
        fulfillmentPort.save(fulfillment);

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PREPARE_SHIPPING, adminId, null));
        orderStore.save(order);

        return new ShippingResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
    }

    /**
     * 배송 출발. SHIPPING_PREPARING → SHIPPED.
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public ShippingResult markShipped(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.markShipped();

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.SHIP, adminId, null));
        orderStore.save(order);

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        return new ShippingResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
    }

    /**
     * 배송 완료. SHIPPED → DELIVERED.
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public ShippingResult markDelivered(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.markDelivered();

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.DELIVER, adminId, null));
        orderStore.save(order);

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        return new ShippingResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
    }

    /** 배송 관련 서비스 작업의 결과를 컨트롤러에 전달하는 내부 DTO. */
    public record ShippingResult(Long orderId, OrderStatus status, java.time.LocalDate expectedShipDate) {}
}
