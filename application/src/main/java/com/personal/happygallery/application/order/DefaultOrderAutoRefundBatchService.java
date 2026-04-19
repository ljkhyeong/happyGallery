package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.OrderAutoRefundBatchUseCase;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 24시간 초과 자동환불 배치 서비스.
 *
 * <p>{@code approval_deadline_at < now} 인 {@link OrderStatus#PAID_APPROVAL_PENDING} 주문을 일괄 처리한다.
 * 각 주문에 대해 재고를 복구하고 PG 환불을 호출한 뒤
 * 상태를 {@link OrderStatus#AUTO_REFUND_TIMEOUT}으로 전이한다.
 *
 * <p>{@code @Scheduled} 연결은 §10에서 수행한다. 현재는 서비스만 구현됨.
 */
@Service
public class DefaultOrderAutoRefundBatchService implements OrderAutoRefundBatchUseCase {

    private final OrderReaderPort orderReader;
    private final OrderAutoRefundProcessor orderAutoRefundProcessor;
    private final Clock clock;

    public DefaultOrderAutoRefundBatchService(OrderReaderPort orderReader,
                                       OrderAutoRefundProcessor orderAutoRefundProcessor,
                                       Clock clock) {
        this.orderReader = orderReader;
        this.orderAutoRefundProcessor = orderAutoRefundProcessor;
        this.clock = clock;
    }

    /**
     * 승인 마감이 경과한 주문을 자동환불 처리한다.
     *
     * <ol>
     *   <li>status=PAID_APPROVAL_PENDING AND approvalDeadlineAt &lt; now 조회</li>
     *   <li>각 주문: 재고 복구 → PG 환불 → AUTO_REFUND_TIMEOUT 전이</li>
     * </ol>
     *
     * @return 처리된 건수
     */
    private static final int PAGE_SIZE = 100;

    public BatchResult autoRefundExpired() {
        LocalDateTime now = LocalDateTime.now(clock);

        return BatchExecutor.executePaginated(
                () -> orderReader.findByStatusAndApprovalDeadlineAtBefore(
                        OrderStatus.PAID_APPROVAL_PENDING, now, PageRequest.ofSize(PAGE_SIZE)),
                Order::getId,
                order -> orderAutoRefundProcessor.process(order.getId(), now),
                "주문 자동환불");
    }
}
