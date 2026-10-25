package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.application.notification.ReviewNotificationPublisher;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.time.Clock;
import java.time.LocalDateTime;
import com.personal.happygallery.domain.notification.NotificationEventType;
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
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;
    private final OrderNotificationSupport orderNotificationSupport;
    private final ReviewNotificationPublisher reviewNotificationPublisher;
    private final OrderRewardAccrualService rewardAccrualService;
    private final Clock clock;

    public DefaultOrderShippingService(OrderReaderPort orderReader,
                                       FulfillmentPort fulfillmentPort,
                                       OrderHistoryPort orderHistoryPort,
                                       OrderNotificationSupport orderNotificationSupport,
                                       ReviewNotificationPublisher reviewNotificationPublisher,
                                       OrderRewardAccrualService rewardAccrualService,
                                       Clock clock) {
        this.orderReader = orderReader;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
        this.orderNotificationSupport = orderNotificationSupport;
        this.reviewNotificationPublisher = reviewNotificationPublisher;
        this.rewardAccrualService = rewardAccrualService;
        this.clock = clock;
    }

    /**
     * 배송 준비 시작. APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING.
     * 결제 confirm에서 생성된 Fulfillment가 SHIPPING 타입이어야 한다.
     *
     * <p>고객 배송지 변경({@link DefaultOrderShippingAddressService})이 주문 행을 잠근 뒤 배송 준비 전 상태인지
     * 검사하므로, 이 전이도 주문 행을 잠가 두 작업을 직렬화한다. 출발·완료 전이는 배송지 변경과 경합하지 않아
     * 낙관 잠금만 쓴다.
     */
    @Override
    @OptimisticLockRetryable
    public ShippingResult prepareShipping(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrderForUpdate(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requireShippingType();
        order.markShippingPreparing();

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PREPARE_SHIPPING, adminId, null));

        return ShippingResult.of(order, fulfillment);
    }

    /**
     * 배송 출발. SHIPPING_PREPARING → SHIPPED.
     */
    @Override
    @OptimisticLockRetryable
    public ShippingResult markShipped(
            Long orderId,
            ShippingCarrier carrierCode,
            String trackingNumber,
            Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);
        fulfillment.requireShippingType();
        order.markShipped();
        fulfillment.recordShipment(carrierCode, trackingNumber, LocalDateTime.now(clock));

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.SHIP, adminId, null));
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
        rewardAccrualService.accrueForCompletion(order);
        reviewNotificationPublisher.requestForOrder(order.getUserId(), order.getId());

        return ShippingResult.of(order, fulfillment);
    }
}
