package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasedItem;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.OrderService.OrderCreationResult;
import com.personal.happygallery.application.order.OrderService.OrderItemRequest;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderOption;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.order.OrderOptionSnapshot;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderFulfiller implements PaymentFulfiller {

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final OrderService orderService;
    private final CartUseCase cartUseCase;
    private final CouponRedemptionUseCase couponRedemptionUseCase;
    private final RewardBenefitService rewardBenefitService;
    private final Clock clock;

    public OrderFulfiller(VerifiedGuestResolver verifiedGuestResolver,
                          OrderService orderService,
                          CartUseCase cartUseCase,
                          CouponRedemptionUseCase couponRedemptionUseCase,
                          RewardBenefitService rewardBenefitService,
                          Clock clock) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.orderService = orderService;
        this.cartUseCase = cartUseCase;
        this.couponRedemptionUseCase = couponRedemptionUseCase;
        this.rewardBenefitService = rewardBenefitService;
        this.clock = clock;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    public void validateStoredPayload(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        validateOrderPayload(attempt, payload);
    }

    private void validateOrderPayload(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        if (!(payload instanceof PreparedOrderPayload op)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문 단가 정보가 없습니다. 결제를 다시 준비해 주세요.");
        }
        if (op.pricing() == null) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "저장된 주문 혜택 금액이 없습니다. 결제를 다시 준비해 주세요.");
        }
        long preparedAmount = 0L;
        long couponDiscountAmount = 0L;
        long rewardUsedAmount = 0L;
        long netPaidAmount = 0L;
        boolean containsMadeToOrder = false;
        for (PreparedOrderItem item : op.items()) {
            if (item.productName() == null || item.productName().isBlank()) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "주문 상품명 정보가 없습니다. 결제를 다시 준비해 주세요.");
            }
            if (item.productType() == null || item.pricing() == null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "저장된 주문 상품 정보가 없습니다. 결제를 다시 준비해 주세요.");
            }
            if (item.productType() == ProductType.MADE_TO_ORDER) {
                containsMadeToOrder = true;
                requireMadeToOrderTerms(item);
            } else if (item.productionLeadDays() != null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT,
                        "기성품 결제에 제작 기간이 저장되어 있습니다. 결제를 다시 준비해 주세요.");
            }
            requireOptionPricing(item);
            long grossAmount = OrderAmountCalculator.addLine(0L, item.qty(), item.unitPrice());
            preparedAmount = safeAdd(preparedAmount, grossAmount);
            OrderItemPricing itemPricing = item.pricing();
            if (itemPricing.grossAmount() != grossAmount) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "저장된 품목별 혜택 금액이 상품 금액과 일치하지 않습니다.");
            }
            couponDiscountAmount = safeAdd(
                    couponDiscountAmount, itemPricing.couponDiscountAmount());
            rewardUsedAmount = safeAdd(
                    rewardUsedAmount, itemPricing.rewardUsedAmount());
            netPaidAmount = safeAdd(netPaidAmount, itemPricing.netPaidAmount());
        }
        if (containsMadeToOrder != (op.madeToOrderConsent() != null)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "주문제작 상품 유형과 동의 정보가 일치하지 않습니다. 결제를 다시 준비해 주세요.");
        }
        OrderPricingSnapshot pricing = op.pricing();
        if (pricing.productAmount() != preparedAmount
                || pricing.shippingFee() != op.shippingFee()
                || pricing.couponDiscountAmount() != couponDiscountAmount
                || pricing.rewardUsedAmount() != rewardUsedAmount
                || pricing.rewardEarnBase() != netPaidAmount
                || pricing.pgPaidAmount() != attempt.getAmount()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 주문 금액이 결제 금액과 일치하지 않습니다.");
        }
        if (op.userId() == null
                && (pricing.issuedCouponId() != null || pricing.rewardUsedAmount() != 0L)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 주문에는 쿠폰과 적립금을 적용할 수 없습니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        validateOrderPayload(attempt, payload);
        PreparedOrderPayload op = (PreparedOrderPayload) payload;
        OrderPricingSnapshot pricing = op.pricing();
        List<OrderItemRequest> orderItems = op.items().stream()
                .map(item -> new OrderItemRequest(
                        item.productId(),
                        item.productVariantId(),
                        item.productName(),
                        item.productType(),
                        item.qty(),
                        item.unitPrice(),
                        item.basePrice(),
                        item.variantPriceAdjustment(),
                        item.textOptionPriceAdjustment(),
                        item.optionSnapshots().stream().map(OrderFulfiller::toOrderOption).toList(),
                        item.specification(),
                        item.careInstructions(),
                        item.productionLeadDays(),
                        item.pricing()))
                .toList();

        if (op.userId() != null) {
            Order order = orderService.createMemberOrder(
                    op.userId(), orderItems, op.fulfillmentType(), op.shippingAddress(),
                    op.madeToOrderConsent(), pricing);
            order.recordPaymentKey(attempt.getConfirmedPaymentKey());
            couponRedemptionUseCase.redeem(
                    pricing.issuedCouponId(), attempt.getId(), order.getId());
            rewardBenefitService.consume(
                    attempt.getId(), order.getId(), pricing.rewardUsedAmount(), LocalDateTime.now(clock));
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

    private static OrderOptionSnapshot toOrderOption(PreparedOrderOption option) {
        return new OrderOptionSnapshot(
                option.type(), option.groupName(), option.value(),
                option.priceAdjustment(), option.sortOrder());
    }

    private static void requireOptionPricing(PreparedOrderItem item) {
        long expectedUnitPrice;
        try {
            expectedUnitPrice = Math.addExact(
                    Math.addExact(item.basePrice(), item.variantPriceAdjustment()),
                    item.textOptionPriceAdjustment());
        } catch (ArithmeticException exception) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "저장된 주문 옵션 금액이 허용 범위를 초과했습니다.");
        }
        if (item.basePrice() < 1L
                || item.textOptionPriceAdjustment() < 0L
                || expectedUnitPrice != item.unitPrice()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "저장된 주문 옵션 금액이 상품 단가와 일치하지 않습니다.");
        }
        long textAdjustment = 0L;
        for (PreparedOrderOption option : item.optionSnapshots()) {
            if (option == null || option.type() == null
                    || option.groupName() == null || option.groupName().isBlank()
                    || option.value() == null || option.value().isBlank()
                    || option.priceAdjustment() < 0L) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "저장된 주문 옵션 정보가 올바르지 않습니다.");
            }
            if (option.type() == ProductOptionType.TEXT) {
                textAdjustment = safeAdd(textAdjustment, option.priceAdjustment());
            }
        }
        if (textAdjustment != item.textOptionPriceAdjustment()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "직접입력형 옵션 금액이 저장된 상품 단가와 일치하지 않습니다.");
        }
        if (item.productType() == ProductType.READY_STOCK
                && (item.productVariantId() != null
                    || item.variantPriceAdjustment() != 0L
                    || item.textOptionPriceAdjustment() != 0L
                    || !item.optionSnapshots().isEmpty())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "기성품 결제에는 주문제작 옵션을 저장할 수 없습니다.");
        }
    }

    private static long safeAdd(long left, long right) {
        try {
            long result = Math.addExact(left, right);
            PaymentAmountPolicy.requireValid(result);
            return result;
        } catch (ArithmeticException exception) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "저장된 주문 금액이 허용 범위를 초과했습니다.");
        }
    }

    private static void requireMadeToOrderTerms(PreparedOrderItem item) {
        if (item.productVariantId() == null
                || item.productionLeadDays() == null
                || item.productionLeadDays() < Product.MIN_PRODUCTION_LEAD_DAYS
                || item.productionLeadDays() > Product.MAX_PRODUCTION_LEAD_DAYS
                || item.specification() == null
                || item.specification().isBlank()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "저장된 주문제작 사양 정보가 올바르지 않습니다. 결제를 다시 준비해 주세요.");
        }
    }
}
