package com.personal.happygallery.app.order;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class OrderAutoRefundBatchService {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoRefundBatchService.class);

    private final OrderRepository orderRepository;
    private final OrderApprovalService orderApprovalService;
    private final NotificationService notificationService;
    private final Clock clock;

    public OrderAutoRefundBatchService(OrderRepository orderRepository,
                                       OrderApprovalService orderApprovalService,
                                       NotificationService notificationService,
                                       Clock clock) {
        this.orderRepository = orderRepository;
        this.orderApprovalService = orderApprovalService;
        this.notificationService = notificationService;
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

        for (Order order : expired) {
            orderApprovalService.restoreInventory(order);
            orderApprovalService.processRefund(order);
            order.markAutoRefunded();
            orderRepository.save(order);
            notificationService.notifyByGuestId(order.getGuestId(), NotificationEventType.ORDER_REFUNDED);
            log.info("주문 자동환불 처리 [orderId={}]", order.getId());
        }

        log.info("자동환불 배치 완료: {}건 처리", expired.size());
        return expired.size();
    }
}
