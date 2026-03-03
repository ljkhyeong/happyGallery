package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 픽업 마감 초과 자동환불 배치 서비스 (§8.4).
 *
 * <p>{@code pickup_deadline_at < now} 인 {@link OrderStatus#PICKUP_READY} 이행 건을 일괄 처리한다.
 * 각 주문에 대해 재고를 복구하고 PG 환불을 호출한 뒤
 * 상태를 {@link OrderStatus#PICKUP_EXPIRED_REFUNDED}로 전이한다.
 *
 * <p>{@code @Scheduled} 연결은 §10에서 수행한다. 현재는 서비스만 구현됨.
 */
@Service
@Transactional
public class PickupExpireBatchService {

    private static final Logger log = LoggerFactory.getLogger(PickupExpireBatchService.class);

    private final FulfillmentRepository fulfillmentRepository;
    private final OrderRepository orderRepository;
    private final OrderApprovalService orderApprovalService;
    private final Clock clock;

    public PickupExpireBatchService(FulfillmentRepository fulfillmentRepository,
                                    OrderRepository orderRepository,
                                    OrderApprovalService orderApprovalService,
                                    Clock clock) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderRepository = orderRepository;
        this.orderApprovalService = orderApprovalService;
        this.clock = clock;
    }

    /**
     * 픽업 마감이 경과한 주문을 자동환불 처리한다.
     *
     * <ol>
     *   <li>Fulfillment.status=PICKUP_READY AND pickupDeadlineAt &lt; now 조회</li>
     *   <li>각 주문: 재고 복구 → PG 환불 → PICKUP_EXPIRED_REFUNDED 전이</li>
     * </ol>
     *
     * @return 처리된 건수
     */
    public int expirePickups() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Fulfillment> expired = fulfillmentRepository
                .findByStatusAndPickupDeadlineAtBefore(OrderStatus.PICKUP_READY, now);

        for (Fulfillment fulfillment : expired) {
            Order order = orderRepository.findById(fulfillment.getOrderId())
                    .orElseThrow(() -> new NotFoundException("주문"));

            orderApprovalService.restoreInventory(order);
            orderApprovalService.processRefund(order);
            order.markPickupExpired();
            fulfillment.syncStatus(order.getStatus());

            orderRepository.save(order);
            fulfillmentRepository.save(fulfillment);
            log.info("픽업 만료 처리 [orderId={}, fulfillmentId={}]", order.getId(), fulfillment.getId());
        }

        log.info("픽업 만료 배치 완료: {}건 처리", expired.size());
        return expired.size();
    }
}
