package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasedItem;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.OrderService.OrderCreationResult;
import com.personal.happygallery.application.order.OrderService.OrderItemRequest;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
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
    public void validateStoredPayload(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        validateOrderPayload(attempt, payload, true);
    }

    private void validateOrderPayload(PaymentAttempt attempt,
                                      PreparedPaymentPayload payload,
                                      boolean deferApprovedLegacyFailure) {
        if (!(payload instanceof PreparedOrderPayload op)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문 단가 정보가 없습니다. 결제를 다시 준비해 주세요.");
        }
        long preparedAmount = 0L;
        boolean containsMadeToOrder = false;
        boolean containsLegacyMadeToOrder = false;
        for (PreparedOrderItem item : op.items()) {
            if (item.productName() == null || item.productName().isBlank()) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "주문 상품명 정보가 없습니다. 결제를 다시 준비해 주세요.");
            }
            if (item.productType() == null) {
                if (op.madeToOrderConsent() != null) {
                    containsLegacyMadeToOrder = true;
                    // APPROVED는 fulfillment 실패로 넘겨 기존 PG 보상 환불 경계를 사용한다.
                    if (!deferApprovedLegacyFailure
                            || attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
                        throw legacyMadeToOrderPayload();
                    }
                }
            } else if (item.productType() == ProductType.MADE_TO_ORDER) {
                containsMadeToOrder = true;
                requireMadeToOrderTerms(item);
            } else if (item.productionLeadDays() != null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT,
                        "기성품 결제에 제작 기간이 저장되어 있습니다. 결제를 다시 준비해 주세요.");
            }
            preparedAmount = OrderAmountCalculator.addLine(
                    preparedAmount, item.qty(), item.unitPrice());
        }
        if (!containsLegacyMadeToOrder
                && containsMadeToOrder != (op.madeToOrderConsent() != null)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "주문제작 상품 유형과 동의 정보가 일치하지 않습니다. 결제를 다시 준비해 주세요.");
        }
        preparedAmount = OrderAmountCalculator.addShippingFee(preparedAmount, op.shippingFee());
        if (preparedAmount != attempt.getAmount()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 주문 금액이 결제 금액과 일치하지 않습니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        validateOrderPayload(attempt, payload, false);
        PreparedOrderPayload op = (PreparedOrderPayload) payload;
        List<OrderItemRequest> orderItems = op.items().stream()
                .map(item -> new OrderItemRequest(
                        item.productId(),
                        item.productName(),
                        normalizeLegacyReadyStockType(item),
                        item.qty(),
                        item.unitPrice(),
                        item.specification(),
                        item.careInstructions(),
                        item.productionLeadDays()))
                .toList();

        if (op.userId() != null) {
            Order order = orderService.createMemberOrder(
                    op.userId(), orderItems, op.fulfillmentType(), op.shippingAddress(),
                    op.shippingFee(), op.madeToOrderConsent());
            order.recordPaymentKey(attempt.getConfirmedPaymentKey());
            if (op.cartCheckout()) {
                cartUseCase.removePurchasedItems(op.userId(), op.items().stream()
                        .map(item -> new PurchasedItem(item.cartItemId(), item.qty()))
                        .toList());
            }
            return new FulfillResult(order.getId(), null);
        }
        Guest guest = verifiedGuestResolver.resolveWithPaymentProof(
                PaymentContext.ORDER,
                attempt.getOrderIdExternal(),
                op.phone(),
                op.guestVerificationProof(),
                op.name());
        OrderCreationResult result = orderService.createPaidOrder(
                guest.getId(), orderItems, op.fulfillmentType(), op.shippingAddress(),
                op.shippingFee(), op.madeToOrderConsent());
        result.order().recordPaymentKey(attempt.getConfirmedPaymentKey());
        return new FulfillResult(result.order().getId(), result.rawAccessToken());
    }

    private static ProductType normalizeLegacyReadyStockType(PreparedOrderItem item) {
        return item.productType() == null ? ProductType.READY_STOCK : item.productType();
    }

    private static void requireMadeToOrderTerms(PreparedOrderItem item) {
        if (item.productionLeadDays() == null
                || item.productionLeadDays() < Product.MIN_PRODUCTION_LEAD_DAYS
                || item.productionLeadDays() > Product.MAX_PRODUCTION_LEAD_DAYS
                || item.specification() == null
                || item.specification().isBlank()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "저장된 주문제작 사양 정보가 올바르지 않습니다. 결제를 다시 준비해 주세요.");
        }
    }

    private static HappyGalleryException legacyMadeToOrderPayload() {
        return new HappyGalleryException(
                ErrorCode.INVALID_INPUT,
                "주문제작 구매 조건이 저장되지 않은 이전 결제입니다. 결제를 다시 준비해 주세요.");
    }
}
