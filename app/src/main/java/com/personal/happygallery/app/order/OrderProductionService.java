package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
public class OrderProductionService {

    private final OrderRepository orderRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;

    public OrderProductionService(OrderRepository orderRepository,
                                  FulfillmentRepository fulfillmentRepository,
                                  OrderApprovalHistoryRepository orderApprovalHistoryRepository) {
        this.orderRepository = orderRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
    }

    /**
     * 예상 출고일을 설정·갱신한다.
     *
     * @param orderId          주문 ID
     * @param expectedShipDate 예상 출고일
     * @return 주문 상태 + 갱신된 출고일
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, random = true))
    public ProductionResult setExpectedShipDate(Long orderId, LocalDate expectedShipDate) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));

        fulfillment.setExpectedShipDate(expectedShipDate);
        fulfillmentRepository.save(fulfillment);

        return new ProductionResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
    }

    /**
     * 고객 동의 후 배송 지연 상태({@link OrderStatus#DELAY_REQUESTED})로 전환한다.
     * {@link OrderStatus#IN_PRODUCTION} 상태가 아니면 400을 던진다.
     *
     * @param orderId 주문 ID
     * @return 전이된 주문 상태 + 출고일
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, random = true))
    public ProductionResult requestDelay(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.requestDelay();

        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        fulfillment.syncStatus(order.getStatus());
        fulfillmentRepository.save(fulfillment);

        orderApprovalHistoryRepository.save(new OrderApprovalHistory(order.getId(), OrderApprovalDecision.DELAY));
        orderRepository.save(order);
        return new ProductionResult(order.getId(), order.getStatus(), fulfillment.getExpectedShipDate());
    }

    /** 제작 관련 서비스 작업의 결과를 컨트롤러에 전달하는 내부 DTO. */
    public record ProductionResult(Long orderId, OrderStatus status, LocalDate expectedShipDate) {}
}
