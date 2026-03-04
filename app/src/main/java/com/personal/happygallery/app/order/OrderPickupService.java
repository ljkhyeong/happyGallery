package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
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
public class OrderPickupService {

    private final OrderRepository orderRepository;
    private final FulfillmentRepository fulfillmentRepository;

    public OrderPickupService(OrderRepository orderRepository,
                              FulfillmentRepository fulfillmentRepository) {
        this.orderRepository = orderRepository;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    /**
     * 픽업 준비 완료. {@link OrderStatus#APPROVED_FULFILLMENT_PENDING} → {@link OrderStatus#PICKUP_READY}.
     * Fulfillment 레코드를 생성하고 픽업 마감 시각을 설정한다.
     *
     * @param orderId          주문 ID
     * @param pickupDeadlineAt 픽업 마감 시각
     * @return 픽업 결과 (주문 ID, 상태, 마감 시각)
     */
    public PickupResult markPickupReady(Long orderId, LocalDateTime pickupDeadlineAt) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.markPickupReady();

        Fulfillment fulfillment = fulfillmentRepository.save(
                new Fulfillment(order.getId(), OrderStatus.PICKUP_READY, pickupDeadlineAt));
        orderRepository.save(order);

        return new PickupResult(order.getId(), order.getStatus(), fulfillment.getPickupDeadlineAt());
    }

    /**
     * 픽업 완료. {@link OrderStatus#PICKUP_READY} → {@link OrderStatus#PICKED_UP}.
     *
     * @param orderId 주문 ID
     * @return 픽업 결과 (주문 ID, 상태, 마감 시각)
     */
    public PickupResult confirmPickup(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("주문"));
        order.confirmPickup();

        Fulfillment fulfillment = fulfillmentRepository.findByOrderIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("이행 정보"));
        fulfillment.syncStatus(order.getStatus());
        fulfillmentRepository.save(fulfillment);
        orderRepository.save(order);

        return new PickupResult(order.getId(), order.getStatus(), fulfillment.getPickupDeadlineAt());
    }

    /** 픽업 관련 서비스 작업의 결과를 컨트롤러에 전달하는 내부 DTO. */
    public record PickupResult(Long orderId, OrderStatus status, LocalDateTime pickupDeadlineAt) {}
}
