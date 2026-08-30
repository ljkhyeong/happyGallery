package com.personal.happygallery.application.order;

import com.personal.happygallery.application.config.OptimisticLockRetryable;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderDelayDecision;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultOrderCustomerActionService implements OrderCustomerActionUseCase {

    private final OrderReaderPort orderReader;
    private final OrderStorePort orderStore;
    private final OrderHistoryPort orderHistoryPort;
    private final OrderRefundSupport orderRefundSupport;
    private final GuestTokenService guestTokenService;

    public DefaultOrderCustomerActionService(OrderReaderPort orderReader,
                                             OrderStorePort orderStore,
                                             OrderHistoryPort orderHistoryPort,
                                             OrderRefundSupport orderRefundSupport,
                                             GuestTokenService guestTokenService) {
        this.orderReader = orderReader;
        this.orderStore = orderStore;
        this.orderHistoryPort = orderHistoryPort;
        this.orderRefundSupport = orderRefundSupport;
        this.guestTokenService = guestTokenService;
    }

    @Override
    @OptimisticLockRetryable
    public ActionResult cancelGuestOrder(Long orderId, String accessToken) {
        return cancel(requireGuestOrder(orderId, accessToken));
    }

    @Override
    @OptimisticLockRetryable
    public ActionResult cancelMemberOrder(Long orderId, Long userId) {
        return cancel(requireMemberOrder(orderId, userId));
    }

    @Override
    @OptimisticLockRetryable
    public ActionResult respondToGuestDelay(
            Long orderId, String accessToken, OrderDelayDecision decision) {
        return respondToDelay(requireGuestOrder(orderId, accessToken), decision);
    }

    @Override
    @OptimisticLockRetryable
    public ActionResult respondToMemberDelay(
            Long orderId, Long userId, OrderDelayDecision decision) {
        return respondToDelay(requireMemberOrder(orderId, userId), decision);
    }

    private ActionResult cancel(Order order) {
        order.cancelByCustomer();
        Refund refund = orderRefundSupport.refundOrder(order);
        orderHistoryPort.save(new OrderApprovalHistory(
                order.getId(), OrderApprovalDecision.CUSTOMER_CANCEL));
        return new ActionResult(orderStore.save(order), refund);
    }

    private ActionResult respondToDelay(Order order, OrderDelayDecision decision) {
        order.respondToDelay(decision);
        if (decision == OrderDelayDecision.ACCEPT) {
            orderHistoryPort.save(new OrderApprovalHistory(
                    order.getId(), OrderApprovalDecision.DELAY_ACCEPT));
            return new ActionResult(orderStore.save(order), null);
        }

        Refund refund = orderRefundSupport.refundOrder(order);
        orderHistoryPort.save(new OrderApprovalHistory(
                order.getId(), OrderApprovalDecision.DELAY_REJECT));
        return new ActionResult(orderStore.save(order), refund);
    }

    private Order requireGuestOrder(Long orderId, String accessToken) {
        String tokenHash = guestTokenService.resolveTokenHash(accessToken);
        return orderReader.findById(orderId)
                .filter(order -> Objects.equals(order.getAccessToken(), tokenHash))
                .orElseThrow(NotFoundException.supplier("주문"));
    }

    private Order requireMemberOrder(Long orderId, Long userId) {
        return orderReader.findById(orderId)
                .filter(order -> Objects.equals(order.getUserId(), userId))
                .orElseThrow(NotFoundException.supplier("주문"));
    }
}
