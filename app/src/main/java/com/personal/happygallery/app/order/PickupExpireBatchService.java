package com.personal.happygallery.app.order;

import com.personal.happygallery.app.batch.BatchExecutor;
import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

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
public class PickupExpireBatchService {

    private final FulfillmentRepository fulfillmentRepository;
    private final PickupExpireProcessor pickupExpireProcessor;
    private final Clock clock;

    public PickupExpireBatchService(FulfillmentRepository fulfillmentRepository,
                                    PickupExpireProcessor pickupExpireProcessor,
                                    Clock clock) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.pickupExpireProcessor = pickupExpireProcessor;
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
    public BatchResult expirePickups() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Fulfillment> expired = fulfillmentRepository
                .findByStatusAndPickupDeadlineAtBefore(OrderStatus.PICKUP_READY, now);

        return BatchExecutor.execute(expired,
                Fulfillment::getOrderId,
                f -> pickupExpireProcessor.process(f.getOrderId(), now),
                "픽업 만료");
    }
}
