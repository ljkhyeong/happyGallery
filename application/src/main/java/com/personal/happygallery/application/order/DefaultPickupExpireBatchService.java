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
 * 픽업 마감 초과 처리 배치 서비스 (§8.4).
 *
 * <p>{@code pickup_deadline_at < now} 인 {@code PICKUP_READY} 주문을 일괄 처리한다.
 * 기성품 주문은 재고 복구와 환불을 요청하고, 주문제작 주문은 환불 없이
 * {@code PICKUP_FORFEITED}로 전이한다.
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
     * 픽업 마감이 경과한 주문을 만료 처리한다.
     *
     * <ol>
     *   <li>Order.status=PICKUP_READY AND pickupDeadlineAt &lt; now 조회</li>
     *   <li>기성품 주문: 재고 복구 → 환불 요청 → PICKUP_EXPIRED 전이</li>
     *   <li>주문제작 주문: 환불 없이 PICKUP_FORFEITED 전이</li>
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
