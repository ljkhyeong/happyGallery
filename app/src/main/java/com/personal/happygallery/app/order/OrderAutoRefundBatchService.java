package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 24시간 초과 자동환불 배치 서비스.
 *
 * <p>{@code approval_deadline_at < now} 인 {@link OrderStatus#PAID_APPROVAL_PENDING} 주문을 일괄 처리한다.
 * 각 주문에 대해 재고를 복구하고 PG 환불을 호출한 뒤
 * 상태를 {@link OrderStatus#AUTO_REFUNDED_TIMEOUT}으로 전이한다.
 *
 * <p>{@code @Scheduled} 연결은 §10에서 수행한다. 현재는 서비스만 구현됨.
 */
@Service
public class OrderAutoRefundBatchService {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoRefundBatchService.class);

    private final OrderRepository orderRepository;
    private final OrderAutoRefundProcessor orderAutoRefundProcessor;
    private final Clock clock;

    public OrderAutoRefundBatchService(OrderRepository orderRepository,
                                       OrderAutoRefundProcessor orderAutoRefundProcessor,
                                       Clock clock) {
        this.orderRepository = orderRepository;
        this.orderAutoRefundProcessor = orderAutoRefundProcessor;
        this.clock = clock;
    }

    /**
     * 승인 마감이 경과한 주문을 자동환불 처리한다.
     *
     * <ol>
     *   <li>status=PAID_APPROVAL_PENDING AND approvalDeadlineAt &lt; now 조회</li>
     *   <li>각 주문: 재고 복구 → PG 환불 → AUTO_REFUNDED_TIMEOUT 전이</li>
     * </ol>
     *
     * @return 처리된 건수
     */
    public int autoRefundExpired() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Order> expired = orderRepository.findByStatusAndApprovalDeadlineAtBefore(
                OrderStatus.PAID_APPROVAL_PENDING, now);
        int processed = 0;

        for (Order candidate : expired) {
            try {
                if (orderAutoRefundProcessor.process(candidate.getId(), now)) {
                    log.info("주문 자동환불 처리 [orderId={}]", candidate.getId());
                    processed++;
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("주문 자동환불 충돌로 스킵 [orderId={}]", candidate.getId());
            }
        }

        log.info("자동환불 배치 완료: {}건 처리", processed);
        return processed;
    }
}
