package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.RetryConfig;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

/**
 * 픽업 이행 관리 서비스 (§8.4).
 *
 * <ul>
 *   <li>{@link #markPickupReady(Long, LocalDateTime)} — 픽업 준비 완료 → {@link OrderStatus#PICKUP_READY}</li>
 *   <li>{@link #confirmPickup(Long)} — 픽업 완료 → {@link OrderStatus#PICKED_UP}</li>
 * </ul>
 */
@Service
@Transactional
public class DefaultOrderPickupService implements OrderPickupUseCase {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final FulfillmentPort fulfillmentPort;

    public DefaultOrderPickupService(OrderReaderPort orderReader,
                                     OrderStorePort orderStore,
                                     FulfillmentPort fulfillmentPort) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.fulfillmentPort = fulfillmentPort;
    }

    /**
     * 픽업 준비 완료. {@link OrderStatus#APPROVED_FULFILLMENT_PENDING} → {@link OrderStatus#PICKUP_READY}.
     * 기존 Fulfillment가 있으면 픽업용으로 전환하고, 없으면 새로 생성한다.
     *
     * @param orderId          주문 ID
     * @param pickupDeadlineAt 픽업 마감 시각
     * @return 픽업 결과 (주문 ID, 상태, 마감 시각)
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public PickupResult markPickupReady(Long orderId, LocalDateTime pickupDeadlineAt) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.markPickupReady();

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .map(existing -> {
                    existing.convertToPickup(pickupDeadlineAt);
                    return existing;
                })
                .orElseGet(() -> new Fulfillment(orderId, pickupDeadlineAt));
        fulfillmentPort.save(fulfillment);
        orderStore.save(order);

        return PickupResult.of(order, fulfillment);
    }

    /**
     * 픽업 완료. {@link OrderStatus#PICKUP_READY} → {@link OrderStatus#PICKED_UP}.
     *
     * @param orderId 주문 ID
     * @return 픽업 결과 (주문 ID, 상태, 마감 시각)
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                    multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                    random = true))
    public PickupResult confirmPickup(Long orderId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.confirmPickup();

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        fulfillmentPort.save(fulfillment);
        orderStore.save(order);

        return PickupResult.of(order, fulfillment);
    }

}
