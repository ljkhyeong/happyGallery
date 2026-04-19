package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.domain.order.Fulfillment;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 픽업 마감 초과 자동환불 배치 서비스 (§8.4).
 *
 * <p>{@code pickup_deadline_at < now} 인 {@code PICKUP_READY} 주문을 일괄 처리한다.
 * 각 주문에 대해 재고를 복구하고 PG 환불을 호출한 뒤
 * 상태를 {@code PICKUP_EXPIRED}로 전이한다.
 */
@Service
public class DefaultPickupExpireBatchService implements PickupExpireBatchUseCase {

    private final FulfillmentPort fulfillmentPort;
    private final PickupExpireProcessor pickupExpireProcessor;
    private final Clock clock;

    public DefaultPickupExpireBatchService(FulfillmentPort fulfillmentPort,
                                    PickupExpireProcessor pickupExpireProcessor,
                                    Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.pickupExpireProcessor = pickupExpireProcessor;
        this.clock = clock;
    }

    /**
     * 픽업 마감이 경과한 주문을 자동환불 처리한다.
     *
     * <ol>
     *   <li>Order.status=PICKUP_READY AND pickupDeadlineAt &lt; now 조회</li>
     *   <li>각 주문: 재고 복구 → PG 환불 → PICKUP_EXPIRED 전이</li>
     * </ol>
     *
     * @return 처리된 건수
     */
    private static final int PAGE_SIZE = 100;

    public BatchResult expirePickups() {
        LocalDateTime now = LocalDateTime.now(clock);

        return BatchExecutor.executePaginated(
                () -> fulfillmentPort.findExpiredPickups(now, PageRequest.ofSize(PAGE_SIZE)),
                Fulfillment::getOrderId,
                f -> pickupExpireProcessor.process(f.getOrderId(), now),
                "픽업 만료");
    }
}
