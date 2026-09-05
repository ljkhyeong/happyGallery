package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartPurchaseItem;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasableCart;
import com.personal.happygallery.application.coupon.port.in.CouponQuote;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.order.OrderPriceProperties;
import com.personal.happygallery.application.payment.GuestPaymentVerificationService;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderOption;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.product.ProductOptionConfigurationService;
import com.personal.happygallery.application.product.ProductOptions.OptionSnapshot;
import com.personal.happygallery.application.product.ProductOptions.PurchaseRequest;
import com.personal.happygallery.application.product.ProductOptions.ResolvedLine;
import com.personal.happygallery.application.product.ProductOptions.ResolvedPurchase;
import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.order.ProportionalAmountAllocator;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static java.util.stream.Collectors.toMap;

@Component
public class OrderPreparer implements PaymentPreparer {

    private final ProductReaderPort productReader;
    private final InventoryReaderPort inventoryReader;
    private final ProductOptionConfigurationService optionConfigurationService;
    private final CartUseCase cartUseCase;
    private final OrderPriceProperties orderPriceProperties;
    private final GuestPaymentVerificationService guestPaymentVerification;
    private final CouponRedemptionUseCase couponRedemptionUseCase;
    private final RewardBenefitService rewardBenefitService;
    private final Clock clock;

    public OrderPreparer(ProductReaderPort productReader,
                         InventoryReaderPort inventoryReader,
                         ProductOptionConfigurationService optionConfigurationService,
                         CartUseCase cartUseCase,
                         OrderPriceProperties orderPriceProperties,
                         GuestPaymentVerificationService guestPaymentVerification,
                         CouponRedemptionUseCase couponRedemptionUseCase,
                         RewardBenefitService rewardBenefitService,
                         Clock clock) {
        this.productReader = productReader;
        this.inventoryReader = inventoryReader;
        this.optionConfigurationService = optionConfigurationService;
        this.cartUseCase = cartUseCase;
        this.orderPriceProperties = orderPriceProperties;
        this.guestPaymentVerification = guestPaymentVerification;
        this.couponRedemptionUseCase = couponRedemptionUseCase;
        this.rewardBenefitService = rewardBenefitService;
        this.clock = clock;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    public PreparedPayment prepare(String paymentOrderId, PaymentPayload payload, AuthContext auth) {
        if (!(payload instanceof OrderPayload op)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 결제 payload가 아닙니다.");
        }
        if (auth.isMember()) {
            if (op.userId() == null || !op.userId().equals(auth.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "회원 정보가 인증과 일치하지 않습니다.");
            }
        } else {
            if (op.userId() != null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 주문에 회원 정보를 지정할 수 없습니다.");
            }
            if (op.cartCheckout()) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "장바구니 결제는 회원만 사용할 수 있습니다.");
            }
            if (op.issuedCouponId() != null || op.rewardAmount() != 0L) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "쿠폰과 적립금은 회원 주문에만 사용할 수 있습니다.");
            }
            if (op.phone() == null || op.verificationCode() == null || op.name() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 주문은 휴대폰 인증이 필요합니다.");
            }
        }
        if (op.issuedCouponId() != null && op.issuedCouponId() < 1L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "사용할 쿠폰이 올바르지 않습니다.");
        }
        if (op.rewardAmount() < 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "사용할 적립금은 0원 이상이어야 합니다.");
        }
        if (!op.cartCheckout() && (op.expectedCartVersion() != null || op.selectedCartItemIds() != null)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "장바구니 버전과 선택 항목은 장바구니 결제에만 사용할 수 있습니다.");
        }

        List<ItemToPrepare> items;
        if (op.cartCheckout()) {
            items = cartItems(auth.userId(), op.expectedCartVersion(), op.selectedCartItemIds());
        } else if (CollectionUtils.isEmpty(op.items())) {
            items = List.of();
        } else {
            items = mergeDirectItems(op.items());
        }
        if (items.isEmpty()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 항목이 비었습니다.");
        }

        List<Long> productIds = items.stream()
                .map(ItemToPrepare::productId)
                .distinct()
                .toList();
        Map<Long, Product> productsById = productReader.findAllById(productIds)
                .stream()
                .collect(toMap(Product::getId, Function.identity()));
        List<Long> readyStockProductIds = productsById.values().stream()
                .filter(product -> product.getType() == ProductType.READY_STOCK)
                .map(Product::getId)
                .toList();
        Map<Long, Inventory> inventoriesByProductId = inventoryReader.findByProductIdIn(readyStockProductIds)
                .stream()
                .collect(toMap(Inventory::getProductId, Function.identity()));
        Map<Integer, ResolvedLine> resolvedByIndex = optionConfigurationService
                .resolvePurchases(purchaseRequests(items))
                .stream()
                .collect(toMap(ResolvedLine::index, Function.identity()));
        requireAvailableQuantity(items, resolvedByIndex, inventoriesByProductId);
        List<PreparedOrderItem> grossPreparedItems = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            grossPreparedItems.add(prepareItem(
                    items.get(index), resolvedByIndex.get(index), productsById));
        }
        MadeToOrderConsent madeToOrderConsent = madeToOrderConsent(op, productsById);
        long productAmount = 0L;
        for (PreparedOrderItem item : grossPreparedItems) {
            productAmount = OrderAmountCalculator.addLine(productAmount, item.qty(), item.unitPrice());
        }
        long shippingFee = op.fulfillmentType() == FulfillmentType.SHIPPING
                ? orderPriceProperties.shippingFee()
                : 0L;
        LocalDateTime quotedAt = LocalDateTime.now(clock);
        CouponQuote couponQuote = couponRedemptionUseCase.quoteAndLock(
                op.userId(), op.issuedCouponId(), productAmount, quotedAt);
        long rewardAmount = rewardBenefitService.quoteAndLock(
                op.userId(), op.rewardAmount(), couponQuote.discountedProductAmount(), quotedAt);
        OrderPricingSnapshot pricing = new OrderPricingSnapshot(
                productAmount,
                shippingFee,
                couponQuote.discountAmount(),
                rewardAmount,
                OrderAmountCalculator.addShippingFee(
                        couponQuote.discountedProductAmount() - rewardAmount, shippingFee),
                couponQuote.issuedCouponId());
        List<PreparedOrderItem> preparedItems = allocateBenefits(
                grossPreparedItems, couponQuote.discountAmount(), rewardAmount);

        String phone = auth.isMember() ? null : KoreanPhoneNumber.required(op.phone());
        String name = auth.isMember() ? null : PersonalName.required(op.name());
        String guestVerificationProof = auth.isMember()
                ? null
                : guestPaymentVerification.consumeAndIssue(
                        PaymentContext.ORDER, paymentOrderId, phone, op.verificationCode());
        return new PreparedPayment(pricing.pgPaidAmount(), new PreparedOrderPayload(
                op.userId(), phone, guestVerificationProof, name, preparedItems, op.cartCheckout(),
                op.fulfillmentType(), op.shippingAddress(), shippingFee, madeToOrderConsent, pricing));
    }

    private static List<PreparedOrderItem> allocateBenefits(
            List<PreparedOrderItem> items, long couponDiscount, long rewardAmount) {
        List<Long> grossAmounts = items.stream()
                .map(item -> OrderAmountCalculator.addLine(0L, item.qty(), item.unitPrice()))
                .toList();
        List<Long> couponAllocations = ProportionalAmountAllocator.allocate(
                couponDiscount, grossAmounts);
        List<Long> afterCouponAmounts = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            afterCouponAmounts.add(grossAmounts.get(index) - couponAllocations.get(index));
        }
        List<Long> rewardAllocations = ProportionalAmountAllocator.allocate(
                rewardAmount, afterCouponAmounts);

        List<PreparedOrderItem> allocated = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            PreparedOrderItem item = items.get(index);
            long grossAmount = grossAmounts.get(index);
            long itemCouponDiscount = couponAllocations.get(index);
            long itemRewardAmount = rewardAllocations.get(index);
            allocated.add(new PreparedOrderItem(
                    item.cartItemId(),
                    item.productId(),
                    item.productVariantId(),
                    item.productName(),
                    item.qty(),
                    item.unitPrice(),
                    item.effectiveBasePrice(),
                    item.variantPriceAdjustment(),
                    item.textOptionPriceAdjustment(),
                    item.optionSnapshots(),
                    item.specification(),
                    item.careInstructions(),
                    item.productionLeadDays(),
                    item.productType(),
                    new OrderItemPricing(
                            grossAmount,
                            itemCouponDiscount,
                            itemRewardAmount,
                            grossAmount - itemCouponDiscount - itemRewardAmount)));
        }
        return List.copyOf(allocated);
    }

    private MadeToOrderConsent madeToOrderConsent(
            OrderPayload payload, Map<Long, Product> productsById) {
        boolean containsMadeToOrder = productsById.values().stream()
                .anyMatch(product -> product.getType() == ProductType.MADE_TO_ORDER);
        if (!containsMadeToOrder) {
            return null;
        }
        if (!payload.madeToOrderConsent()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문제작 상품의 청약철회 제한 안내에 동의해 주세요.");
        }
        if (!MadeToOrderConsent.CURRENT_VERSION.equals(payload.madeToOrderConsentVersion())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문제작 동의 안내가 변경되었습니다. 새 안내를 확인해 주세요.");
        }
        return MadeToOrderConsent.current(LocalDateTime.now(clock));
    }

    private List<ItemToPrepare> cartItems(Long userId, String expectedCartVersion, List<Long> selectedCartItemIds) {
        if (selectedCartItemIds != null && (expectedCartVersion == null
                || selectedCartItemIds.isEmpty() || selectedCartItemIds.size() > 100
                || selectedCartItemIds.stream().anyMatch(id -> id == null || id < 1)
                || new HashSet<>(selectedCartItemIds).size() != selectedCartItemIds.size())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "선택 구매에는 최신 장바구니 버전과 중복 없는 항목이 필요합니다.");
        }
        PurchasableCart cart = cartUseCase.getPurchasableCart(userId);
        if (expectedCartVersion != null && !expectedCartVersion.equals(cart.cartVersion())) {
            throw new HappyGalleryException(ErrorCode.CART_SNAPSHOT_CHANGED);
        }
        var selectedIds = selectedCartItemIds == null ? null : new HashSet<>(selectedCartItemIds);
        var selectedItems = cart.items().stream()
                .filter(item -> selectedIds == null || selectedIds.contains(item.cartItemId()))
                .toList();
        if (selectedIds != null && selectedItems.size() != selectedIds.size()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "선택한 항목 중 현재 장바구니에서 구매할 수 없는 상품이 있습니다.");
        }
        return selectedItems.stream().map(ItemToPrepare::from).toList();
    }

    private List<ItemToPrepare> mergeDirectItems(List<OrderItemRef> requestedItems) {
        List<ItemToPrepare> items = new ArrayList<>(requestedItems.size());
        for (OrderItemRef item : requestedItems) {
            if (item == null || item.productId() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 상품이 지정되지 않았습니다.");
            }
            OrderAmountCalculator.requireQuantity(item.qty());
            List<TextInput> textInputs = item.textInputs().stream()
                    .map(input -> new TextInput(input.groupKey(), input.value()))
                    .toList();
            items.add(new ItemToPrepare(
                    null, item.productId(), item.productVariantId(), textInputs, item.qty()));
        }
        return List.copyOf(items);
    }

    private PreparedOrderItem prepareItem(
            ItemToPrepare item, ResolvedLine resolvedLine, Map<Long, Product> productsById) {
        Product product = productsById.get(item.productId());
        if (product == null || resolvedLine == null) {
            throw new NotFoundException("상품");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "판매 중인 상품만 주문할 수 있습니다.");
        }
        ResolvedPurchase purchase = resolvedLine.purchase();
        return new PreparedOrderItem(
                item.cartItemId(),
                item.productId(),
                purchase.variantId(),
                product.getName(),
                item.qty(),
                purchase.unitPrice(),
                purchase.basePrice(),
                purchase.variantPriceAdjustment(),
                purchase.textOptionPriceAdjustment(),
                purchase.optionSnapshots().stream().map(OrderPreparer::toPreparedOption).toList(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays(),
                product.getType(),
                null);
    }

    private static List<PurchaseRequest> purchaseRequests(List<ItemToPrepare> items) {
        List<PurchaseRequest> requests = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            ItemToPrepare item = items.get(index);
            requests.add(new PurchaseRequest(
                    index, item.productId(), item.productVariantId(), item.textInputs()));
        }
        return List.copyOf(requests);
    }

    private static void requireAvailableQuantity(
            List<ItemToPrepare> items,
            Map<Integer, ResolvedLine> resolvedByIndex,
            Map<Long, Inventory> inventoriesByProductId) {
        Map<StockKey, Integer> requestedByStock = new HashMap<>();
        for (int index = 0; index < items.size(); index++) {
            ItemToPrepare item = items.get(index);
            ResolvedLine resolved = resolvedByIndex.get(index);
            if (resolved == null) {
                throw new NotFoundException("상품");
            }
            StockKey stockKey;
            int availableQuantity;
            if (resolved.product().getType() == ProductType.MADE_TO_ORDER) {
                stockKey = new StockKey(null, resolved.purchase().variantId());
                availableQuantity = resolved.purchase().availableQuantity();
            } else {
                Inventory inventory = inventoriesByProductId.get(item.productId());
                if (inventory == null) {
                    throw new NotFoundException("재고");
                }
                stockKey = new StockKey(item.productId(), null);
                availableQuantity = inventory.getQuantity();
            }
            int requested;
            try {
                requested = requestedByStock.merge(stockKey, item.qty(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 수량이 너무 큽니다.");
            }
            OrderAmountCalculator.requireQuantity(requested);
            if (requested > availableQuantity) {
                throw new InventoryNotEnoughException();
            }
        }
    }

    private static PreparedOrderOption toPreparedOption(OptionSnapshot option) {
        return new PreparedOrderOption(
                option.type(), option.groupName(), option.value(),
                option.priceAdjustment(), option.sortOrder());
    }

    private record StockKey(Long productId, Long variantId) {}

    private record ItemToPrepare(
            Long cartItemId,
            Long productId,
            Long productVariantId,
            List<TextInput> textInputs,
            int qty) {

        private ItemToPrepare {
            textInputs = List.copyOf(textInputs);
        }

        private static ItemToPrepare from(CartPurchaseItem item) {
            return new ItemToPrepare(
                    item.cartItemId(), item.productId(), item.productVariantId(),
                    item.textInputs(), item.qty());
        }
    }
}
