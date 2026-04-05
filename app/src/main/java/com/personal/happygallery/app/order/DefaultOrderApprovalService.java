package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.config.OptimisticLockRetryable;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.ProductType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 승인/거절 서비스.
 *
 * <ul>
 *   <li>{@link #approve(Long)} — 관리자 승인 → {@link com.personal.happygallery.domain.order.OrderStatus#APPROVED_FULFILLMENT_PENDING}</li>
 *   <li>{@link #reject(Long)} — 관리자 거절 → 재고 복구 → PG 환불 → {@link com.personal.happygallery.domain.order.OrderStatus#REJECTED}</li>
 * </ul>
 *
 * <p>이미 환불된 주문에 대한 승인/거절 시도는
 * {@link com.personal.happygallery.domain.error.AlreadyRefundedException}(409)을 던진다.
 */
@Service
@Transactional
public class DefaultOrderApprovalService implements OrderApprovalUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderApprovalService.class);

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final OrderItemPort orderItemPort;
    private final FulfillmentPort fulfillmentPort;
    private final OrderHistoryPort orderHistoryPort;
    private final OrderRefundSupport orderRefundSupport;

    public DefaultOrderApprovalService(OrderReaderPort orderReader,
                                OrderStorePort orderStore,
                                OrderItemPort orderItemPort,
                                FulfillmentPort fulfillmentPort,
                                OrderHistoryPort orderHistoryPort,
                                OrderRefundSupport orderRefundSupport) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderItemPort = orderItemPort;
        this.fulfillmentPort = fulfillmentPort;
        this.orderHistoryPort = orderHistoryPort;
        this.orderRefundSupport = orderRefundSupport;
    }

    /**
     * 주문을 승인한다. 이미 환불된 주문은 409.
     *
     * <p>주문 내 상품 중 {@link ProductType#MADE_TO_ORDER}가 하나라도 있으면
     * {@link Order#approveAsProduction()}을 호출하여 {@link OrderStatus#IN_PRODUCTION}으로 전이하고
     * Fulfillment 레코드를 생성한다. 그 외에는 {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}으로 전이한다.
     *
     * @param orderId 주문 ID
     * @return 승인된 주문
     */
    @OptimisticLockRetryable
    public Order approve(Long orderId) {
        return approve(orderId, null);
    }

    @OptimisticLockRetryable
    public Order approve(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));

        boolean isMadeToOrder = isMadeToOrderOrder(order);
        if (isMadeToOrder) {
            order.approveAsProduction();
            fulfillmentPort.save(
                    new Fulfillment(order.getId(), FulfillmentType.SHIPPING));
        } else {
            order.approve();
        }
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.APPROVE, adminId, null));
        log.info("order approved [orderId={} adminId={} madeToOrder={}]", orderId, adminId, isMadeToOrder);
        return orderStore.save(order);
    }

    private boolean isMadeToOrderOrder(Order order) {
        return orderItemPort.existsByOrderAndProductType(order, ProductType.MADE_TO_ORDER);
    }

    /**
     * 주문을 거절한다.
     *
     * <ol>
     *   <li>재고 복구</li>
     *   <li>환불 요청 기록 + PG 환불 호출</li>
     *   <li>주문 상태 → {@link com.personal.happygallery.domain.order.OrderStatus#REJECTED}</li>
     * </ol>
     *
     * @param orderId 주문 ID
     * @return 거절된 주문
     */
    @OptimisticLockRetryable
    public Order reject(Long orderId) {
        return reject(orderId, null);
    }

    @OptimisticLockRetryable
    public Order reject(Long orderId, Long adminId) {
        Order order = orderReader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
        order.reject();

        orderRefundSupport.refundOrder(order);
        orderHistoryPort.save(
                new OrderApprovalHistory(order.getId(), OrderApprovalDecision.REJECT, adminId, null));
        log.info("order rejected [orderId={} adminId={}]", orderId, adminId);

        return orderStore.save(order);
    }

}
