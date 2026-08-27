package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.order.OrderStockService.StockAdjustment;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 환불 공통 보조 로직.
 *
 * <p>주문 거절·자동환불의 재고 복구와 환불 요청을 한곳에서 처리한다. 픽업 미수령은
 * 만료 시 재고만 복구하고 관리자 예외 환불 시에는 재고를 다시 복구하지 않는다.
 * 실제 PG 호출과 환불 성공 알림은 부모 트랜잭션 커밋 이후
 * {@link RefundExecutionService}가 처리한다.
 */
@Service
class OrderRefundSupport {

    private final OrderItemPort orderItemPort;
    private final OrderStockService orderStockService;
    private final RefundExecutionService refundExecutionService;
    private final RewardBenefitService rewardBenefitService;

    OrderRefundSupport(OrderItemPort orderItemPort,
                       OrderStockService orderStockService,
                       RefundExecutionService refundExecutionService,
                       RewardBenefitService rewardBenefitService) {
        this.orderItemPort = orderItemPort;
        this.orderStockService = orderStockService;
        this.refundExecutionService = refundExecutionService;
        this.rewardBenefitService = rewardBenefitService;
    }

    /**
     * 재고 복구 → 환불 요청 생성을 순서대로 수행한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    Refund refundOrder(Order order) {
        restoreInventory(order);
        return requestRefund(order);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void restoreInventory(Order order) {
        List<OrderItem> items = orderItemPort.findByOrder(order);
        orderStockService.restoreAll(items.stream()
                .map(item -> new StockAdjustment(
                        item.getProductId(), item.getProductVariantId(), item.getQty()))
                .toList());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Refund refundWithoutInventoryRestore(Order order) {
        return requestRefund(order);
    }

    private Refund requestRefund(Order order) {
        long rewardRevokeAmount = order.getUserId() == null
                ? 0L
                : rewardBenefitService.getEarnedSnapshot(order.getId()).remainingAmount();
        return refundExecutionService.requestOrderRefund(
                order.getId(),
                order.getPgPaidAmount(),
                order.getTotalAmount(),
                order.getRewardUsedAmount(),
                rewardRevokeAmount,
                order.getIssuedCouponId() != null,
                order.getPaymentKey());
    }
}
