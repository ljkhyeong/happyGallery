package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasedItem;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.OrderService.OrderCreationResult;
import com.personal.happygallery.application.order.OrderService.OrderItemRequest;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
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
    private final CartUseCase cartUseCase;

    public OrderFulfiller(VerifiedGuestResolver verifiedGuestResolver,
                          OrderService orderService,
                          CartUseCase cartUseCase) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.orderService = orderService;
        this.cartUseCase = cartUseCase;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    public void validateStoredPayload(PaymentAttempt attempt, PaymentPayload payload) {
        if (!(payload instanceof PreparedOrderPayload op)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문 단가 정보가 없습니다. 결제를 다시 준비해 주세요.");
        }
        long preparedAmount = 0L;
        for (PaymentPayload.PreparedOrderItem item : op.items()) {
            if (item.productName() == null || item.productName().isBlank()) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "주문 상품명 정보가 없습니다. 결제를 다시 준비해 주세요.");
            }
            preparedAmount = OrderAmountCalculator.addLine(
                    preparedAmount, item.qty(), item.unitPrice());
        }
        preparedAmount = OrderAmountCalculator.addShippingFee(preparedAmount, op.shippingFee());
        if (preparedAmount != attempt.getAmount()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 주문 금액이 결제 금액과 일치하지 않습니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentPayload payload, String paymentKey) {
        PreparedOrderPayload op = (PreparedOrderPayload) payload;
        List<OrderItemRequest> orderItems = op.items().stream()
                .map(item -> new OrderItemRequest(
                        item.productId(), item.productName(), item.qty(), item.unitPrice()))
                .toList();

        if (op.userId() != null) {
            Order order = orderService.createMemberOrder(
                    op.userId(), orderItems, op.fulfillmentType(), op.shippingAddress(), op.shippingFee());
            order.recordPaymentKey(paymentKey);
            if (op.cartCheckout()) {
                cartUseCase.removePurchasedItems(op.userId(), op.items().stream()
                        .map(item -> new PurchasedItem(item.cartItemId(), item.qty()))
                        .toList());
            }
            return new FulfillResult(order.getId(), null);
        }
        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(op.phone(), op.verificationCode(), op.name());
        OrderCreationResult result = orderService.createPaidOrder(
                guest.getId(), orderItems, op.fulfillmentType(), op.shippingAddress(), op.shippingFee());
        result.order().recordPaymentKey(paymentKey);
        return new FulfillResult(result.order().getId(), result.rawAccessToken());
    }
}
