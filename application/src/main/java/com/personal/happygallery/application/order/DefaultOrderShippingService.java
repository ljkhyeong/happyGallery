package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.reward.RewardAccrualPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
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
public class DefaultOrderShippingService implements OrderShippingUseCase {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;
    private final OrderNotificationSupport orderNotificationSupport;
    private final RewardBenefitService rewardBenefitService;
    private final Clock clock;

    public DefaultOrderShippingService(OrderReaderPort orderReader,
                                       OrderStorePort orderStore,
                                       FulfillmentPort fulfillmentPort,
                                       OrderHistoryPort orderHistoryPort,
                                       OrderNotificationSupport orderNotificationSupport,
                                       RewardBenefitService rewardBenefitService,
                                       Clock clock) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
        this.orderNotificationSupport = orderNotificationSupport;
        this.rewardBenefitService = rewardBenefitService;
        this.clock = clock;
    }

    /**
     * 배송 준비 시작. APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING.
     * 결제 confirm에서 생성된 Fulfillment가 SHIPPING 타입이어야 한다.
     */
    @Override
    @OptimisticLockRetryable
    public ShippingResult prepareShipping(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requireShippingType();
        order.markShippingPreparing();

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PREPARE_SHIPPING, adminId, null));
        orderStore.save(order);

        return ShippingResult.of(order, fulfillment);
    }

    /**
     * 배송 출발. SHIPPING_PREPARING → SHIPPED.
     */
    @Override
    @OptimisticLockRetryable
    public ShippingResult markShipped(
            Long orderId, String carrier, String trackingNumber, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requireShippingType();
        order.markShipped();
        fulfillment.recordShipment(carrier, trackingNumber);

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.SHIP, adminId, null));
        fulfillmentPort.save(fulfillment);
        orderStore.save(order);
        orderNotificationSupport.notifyCustomer(order, NotificationEventType.ORDER_SHIPPED);

        return ShippingResult.of(order, fulfillment);
    }

    /**
     * 배송 완료. SHIPPED → DELIVERED.
     */
    @Override
    @OptimisticLockRetryable
    public ShippingResult markDelivered(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requireShippingType();
        order.markDelivered();

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.DELIVER, adminId, null));
        orderStore.save(order);
        accrueMemberReward(order);

        return ShippingResult.of(order, fulfillment);
    }

    private void accrueMemberReward(Order order) {
        if (order.getUserId() == null) {
            return;
        }
        rewardBenefitService.accrue(
                order.getUserId(),
                order.getId(),
                RewardAccrualPolicy.calculate(order.getRewardEarnBase()),
                LocalDateTime.now(clock));
    }
}
