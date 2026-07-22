package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase.ProposeDelayCommand;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase.SetExpectedShipDateCommand;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 제작 주문 관리 서비스 (§8.3).
 *
 * <ul>
 *   <li>{@link #setExpectedShipDate(SetExpectedShipDateCommand)} — 예상 출고일 설정/갱신</li>
 *   <li>{@link #proposeDelay(ProposeDelayCommand)} — 제작 지연을 제안하고 고객 응답을 대기</li>
 *   <li>{@link #cancelForDelayRejection(Long, Long)} — 고객 응답 대기 중 관리자 거절 처리</li>
 * </ul>
 *
 * <p>각 메서드는 컨트롤러가 추가 조회 없이 응답을 구성할 수 있도록
 * {@link ProductionResult}를 반환한다.
 */
@Service
@Transactional
public class DefaultOrderProductionService implements OrderProductionUseCase {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;
    private final OrderRefundSupport orderRefundSupport;
    private final OrderNotificationSupport orderNotificationSupport;

    public DefaultOrderProductionService(OrderReaderPort orderReader,
                                         OrderStorePort orderStore,
                                         FulfillmentPort fulfillmentPort,
                                         OrderHistoryPort orderHistoryPort,
                                         OrderRefundSupport orderRefundSupport,
                                         OrderNotificationSupport orderNotificationSupport) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
        this.orderRefundSupport = orderRefundSupport;
        this.orderNotificationSupport = orderNotificationSupport;
    }

    /**
     * 예상 출고일을 설정·갱신한다.
     *
     * @param command 주문 ID, 예상 출고일, 처리 관리자 ID
     * @return 주문 상태 + 갱신된 출고일
     */
    @Override
    @OptimisticLockRetryable
    public ProductionResult setExpectedShipDate(SetExpectedShipDateCommand command) {
        Order order = OrderLookups.requireOrder(orderReader, command.orderId());
        order.getStatus().requireExpectedShipDateWritable();
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, command.orderId());
        LocalDate previousShipDate = fulfillment.getExpectedShipDate();

        fulfillment.setExpectedShipDate(command.expectedShipDate());
        fulfillmentPort.save(fulfillment);
        orderHistoryPort.save(new OrderApprovalHistory(
                order.getId(),
                OrderApprovalDecision.SHIP_DATE_UPDATED,
                command.adminId(),
                shipDateChangeReason(previousShipDate, command.expectedShipDate())));

        return ProductionResult.of(order, fulfillment);
    }

    /**
     * 제작 지연을 제안하고 고객 동의 대기 상태({@link OrderStatus#DELAY_CONSENT_PENDING})로 전환한다.
     * {@link OrderStatus#IN_PRODUCTION} 상태가 아니면 400을 던진다.
     *
     * @param command 주문 ID와 처리 관리자 ID
     * @return 전이된 주문 상태 + 출고일
     */
    @Override
    @OptimisticLockRetryable
    public ProductionResult proposeDelay(ProposeDelayCommand command) {
        Order order = OrderLookups.requireOrder(orderReader, command.orderId());
        order.proposeDelay();

        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, command.orderId());

        orderHistoryPort.save(new OrderApprovalHistory(
                order.getId(), OrderApprovalDecision.DELAY, command.adminId(), null));
        orderStore.save(order);
        orderNotificationSupport.notifyCustomer(order, NotificationEventType.ORDER_DELAY_REQUESTED);
        return ProductionResult.of(order, fulfillment);
    }

    private static String shipDateChangeReason(LocalDate previousShipDate, LocalDate nextShipDate) {
        return "예상 출고일: %s -> %s".formatted(
                previousShipDate == null ? "미설정" : previousShipDate,
                nextShipDate == null ? "미설정" : nextShipDate);
    }

    /**
     * 고객이 제작 지연을 거절했으나 직접 응답하지 못한 경우 관리자가 취소한다.
     * {@link OrderStatus#DELAY_CONSENT_PENDING} 상태에서만 허용한다.
     */
    @Override
    @OptimisticLockRetryable
    public DelayCancellationResult cancelForDelayRejection(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);

        order.cancelForDelayRejection();
        Refund refund = orderRefundSupport.refundOrder(order);

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.DELAY_CANCEL, adminId, null));
        orderStore.save(order);
        return new DelayCancellationResult(ProductionResult.of(order, fulfillment), refund);
    }

    /**
     * 지연 수락 상태에서 제작을 재개한다.
     * {@link OrderStatus#DELAY_ACCEPTED} → {@link OrderStatus#IN_PRODUCTION}.
     */
    @Override
    @OptimisticLockRetryable
    public ProductionResult resumeProduction(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        order.resumeProduction();

        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.RESUME_PRODUCTION, adminId, null));
        orderStore.save(order);
        return ProductionResult.of(order, fulfillment);
    }

    /**
     * 제작 완료 처리. {@link OrderStatus#IN_PRODUCTION} 또는 {@link OrderStatus#DELAY_ACCEPTED}에서
     * {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}으로 전이한다.
     * 이후 픽업 준비({@code markPickupReady}) 또는 배송 흐름으로 이어진다.
     */
    @Override
    @OptimisticLockRetryable
    public ProductionResult completeProduction(Long orderId, Long adminId) {
        Order order = OrderLookups.requireOrder(orderReader, orderId);
        order.completeProduction();

        Fulfillment fulfillment = OrderLookups.requireFulfillment(fulfillmentPort, orderId);

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PRODUCTION_COMPLETE, adminId, null));
        orderStore.save(order);
        return ProductionResult.of(order, fulfillment);
    }
}
