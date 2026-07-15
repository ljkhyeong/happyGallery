package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.OrderService.OrderCreationResult;
import com.personal.happygallery.application.order.OrderService.OrderItemRequest;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderFulfiller implements PaymentFulfiller {

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final OrderService orderService;

    public OrderFulfiller(VerifiedGuestResolver verifiedGuestResolver,
                          OrderService orderService) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.orderService = orderService;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    public void validate(PaymentAttempt attempt, PaymentPayload payload, AuthContext auth) {
        if (payload instanceof OrderPayload) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문 단가 정보가 없습니다. 결제를 다시 준비해 주세요.");
        }
        if (!(payload instanceof PreparedOrderPayload op)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 결제 payload가 아닙니다.");
        }
        if (op.items() == null || op.items().isEmpty()
                || op.items().stream().anyMatch(item -> item == null
                        || item.productId() == null || item.qty() <= 0 || item.unitPrice() < 0)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 주문 항목이 올바르지 않습니다.");
        }
        long preparedAmount = op.items().stream()
                .mapToLong(item -> (long) item.qty() * item.unitPrice())
                .sum();
        if (preparedAmount != attempt.getAmount()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 주문 금액이 결제 금액과 일치하지 않습니다.");
        }
        if (auth.isMember()) {
            if (op.userId() == null || !op.userId().equals(auth.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "회원 정보가 인증과 일치하지 않습니다.");
            }
            return;
        }
        if (op.phone() == null || op.verificationCode() == null || op.name() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 주문은 휴대폰 인증이 필요합니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PaymentPayload payload, AuthContext auth, String paymentKey) {
        PreparedOrderPayload op = (PreparedOrderPayload) payload;
        List<OrderItemRequest> orderItems = op.items().stream()
                .map(item -> new OrderItemRequest(item.productId(), item.qty(), item.unitPrice()))
                .toList();

        if (auth.isMember()) {
            Order order = orderService.createMemberOrder(auth.userId(), orderItems);
            order.recordPaymentKey(paymentKey);
            return new FulfillResult(order.getId(), null);
        }
        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(op.phone(), op.verificationCode(), op.name());
        OrderCreationResult result = orderService.createPaidOrder(guest.getId(), orderItems);
        result.order().recordPaymentKey(paymentKey);
        return new FulfillResult(result.order().getId(), result.rawAccessToken());
    }
}
