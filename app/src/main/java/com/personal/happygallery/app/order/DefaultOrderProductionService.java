package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 제작 주문 관리 서비스 (§8.3).
 *
 * <ul>
 *   <li>{@link #setExpectedShipDate(Long, LocalDate)} — 예상 출고일 설정/갱신</li>
 *   <li>{@link #requestDelay(Long)} — 고객 동의 후 {@link OrderStatus#DELAY_REQUESTED}으로 전환</li>
 * </ul>
 *
 * <p>두 메서드 모두 컨트롤러가 추가 조회 없이 응답을 구성할 수 있도록
 * {@link ProductionResult}를 반환한다.
 */
@Service
@Transactional
public class DefaultOrderProductionService implements OrderProductionUseCase {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;

    public DefaultOrderProductionService(OrderReaderPort orderReader,
                                         OrderStorePort orderStore,
                                         FulfillmentPort fulfillmentPort,
                                         OrderHistoryPort orderHistoryPort) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
    }

    /**
     * 예상 출고일을 설정·갱신한다.
     *
     * @param orderId          주문 ID
     * @param expectedShipDate 예상 출고일
     * @return 주문 상태 + 갱신된 출고일
     */
    @OptimisticLockRetryable
    public ProductionResult setExpectedShipDate(Long orderId, LocalDate expectedShipDate) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
        order.getStatus().requireExpectedShipDateWritable();
        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));
        fulfillment.requireShippingType();

        fulfillment.setExpectedShipDate(expectedShipDate);
        fulfillmentPort.save(fulfillment);

        return ProductionResult.of(order, fulfillment);
    }

    /**
     * 고객 동의 후 배송 지연 상태({@link OrderStatus#DELAY_REQUESTED})로 전환한다.
     * {@link OrderStatus#IN_PRODUCTION} 상태가 아니면 400을 던진다.
     *
     * @param orderId 주문 ID
     * @return 전이된 주문 상태 + 출고일
     */
    @OptimisticLockRetryable
    public ProductionResult requestDelay(Long orderId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
        order.requestDelay();

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));

        orderHistoryPort.save(new OrderApprovalHistory(order.getId(), OrderApprovalDecision.DELAY));
        orderStore.save(order);
        return ProductionResult.of(order, fulfillment);
    }

    /**
     * 지연 요청 상태에서 제작을 재개한다.
     * {@link OrderStatus#DELAY_REQUESTED} → {@link OrderStatus#IN_PRODUCTION}.
     */
    @OptimisticLockRetryable
    public ProductionResult resumeProduction(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
        order.resumeProduction();

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.RESUME_PRODUCTION, adminId, null));
        orderStore.save(order);
        return ProductionResult.of(order, fulfillment);
    }

    /**
     * 제작 완료 처리. {@link OrderStatus#IN_PRODUCTION} 또는 {@link OrderStatus#DELAY_REQUESTED}에서
     * {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}으로 전이한다.
     * 이후 픽업 준비({@code markPickupReady}) 또는 배송 흐름으로 이어진다.
     */
    @OptimisticLockRetryable
    public ProductionResult completeProduction(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
        order.completeProduction();

        Fulfillment fulfillment = fulfillmentPort.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));

        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.PRODUCTION_COMPLETE, adminId, null));
        orderStore.save(order);
        return ProductionResult.of(order, fulfillment);
    }
}
