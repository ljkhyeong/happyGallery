package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 환불 공통 보조 로직.
 *
 * <p>재고 복구 → 환불 요청 순서를 단일 지점에서 강제하여 주문 거절·자동환불·픽업 만료에서
 * 동일한 보상 흐름을 보장한다. 실제 PG 호출과 환불 성공 알림은 부모 트랜잭션 커밋 이후
 * {@link RefundExecutionService}가 처리한다.
 */
@Service
class OrderRefundSupport {

    private final OrderItemPort orderItemPort;
    private final InventoryService inventoryService;
    private final RefundExecutionService refundExecutionService;

    OrderRefundSupport(OrderItemPort orderItemPort,
                       InventoryService inventoryService,
                       RefundExecutionService refundExecutionService) {
        this.orderItemPort = orderItemPort;
        this.inventoryService = inventoryService;
        this.refundExecutionService = refundExecutionService;
    }

    /**
     * 재고 복구 → 환불 요청 생성을 순서대로 수행한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void refundOrder(Order order) {
        List<OrderItem> items = orderItemPort.findByOrder(order);
        for (OrderItem item : items) {
            inventoryService.restore(item.getProductId(), item.getQty());
        }

        refundExecutionService.processOrderRefund(order.getId(), order.getTotalAmount(), order.getPaymentKey());
    }
}
