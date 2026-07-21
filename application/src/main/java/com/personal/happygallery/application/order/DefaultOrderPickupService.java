package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 픽업 이행 관리 서비스 (§8.4).
 *
 * <ul>
 *   <li>{@link #markPickupReady(Long, LocalDateTime, Long)} — 픽업 준비 완료 → {@link OrderStatus#PICKUP_READY}</li>
 *   <li>{@link #confirmPickup(Long, Long)} — 픽업 완료 → {@link OrderStatus#PICKED_UP}</li>
 * </ul>
 */
@Service
@Transactional
public class DefaultOrderPickupService implements OrderPickupUseCase {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;
    private final OrderNotificationSupport orderNotificationSupport;
    private final Clock clock;

    public DefaultOrderPickupService(OrderReaderPort orderReader,
                                     OrderStorePort orderStore,
                                     FulfillmentPort fulfillmentPort,
                                     OrderHistoryPort orderHistoryPort,
                                     OrderNotificationSupport orderNotificationSupport,
                                     Clock clock) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
        this.orderNotificationSupport = orderNotificationSupport;
        this.clock = clock;
    }

    /**
     * 픽업 준비 완료. {@link OrderStatus#APPROVED_FULFILLMENT_PENDING} → {@link OrderStatus#PICKUP_READY}.
     * 결제 confirm에서 생성된 Fulfillment가 PICKUP 타입인지 확인하고 마감 시각을 설정한다.
     *
     * @param orderId          주문 ID
     * @param pickupDeadlineAt 픽업 마감 시각
     * @return 픽업 결과 (주문 ID, 상태, 마감 시각)
     */
    @Override
    @OptimisticLockRetryable
    public PickupResult markPickupReady(Long orderId, LocalDateTime pickupDeadlineAt, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requirePickupType();
        if (pickupDeadlineAt == null || !pickupDeadlineAt.isAfter(LocalDateTime.now(clock))) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "픽업 마감 시각은 현재보다 이후여야 합니다.");
        }
        order.markPickupReady();
        fulfillment.setPickupDeadline(pickupDeadlineAt);
        fulfillmentPort.save(fulfillment);
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PICKUP_READY, adminId, null));
        orderStore.save(order);
        orderNotificationSupport.notifyCustomer(order, NotificationEventType.ORDER_PICKUP_READY);

        return PickupResult.of(order, fulfillment);
    }

    /**
     * 픽업 완료. {@link OrderStatus#PICKUP_READY} → {@link OrderStatus#PICKED_UP}.
     *
     * @param orderId 주문 ID
     * @return 픽업 결과 (주문 ID, 상태, 마감 시각)
     */
    @Override
    @OptimisticLockRetryable
    public PickupResult confirmPickup(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requirePickupType();
        order.confirmPickup();
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PICKUP_COMPLETE, adminId, null));
        orderStore.save(order);

        return PickupResult.of(order, fulfillment);
    }
}
