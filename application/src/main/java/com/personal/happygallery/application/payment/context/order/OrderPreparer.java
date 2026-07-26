package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartPurchaseItem;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.order.OrderPriceProperties;
import com.personal.happygallery.application.payment.GuestPaymentVerificationService;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static java.util.stream.Collectors.toMap;

@Component
public class OrderPreparer implements PaymentPreparer {

    private final ProductReaderPort productReader;
    private final InventoryReaderPort inventoryReader;
    private final CartUseCase cartUseCase;
    private final OrderPriceProperties orderPriceProperties;
    private final GuestPaymentVerificationService guestPaymentVerification;
    private final Clock clock;

    public OrderPreparer(ProductReaderPort productReader,
                         InventoryReaderPort inventoryReader,
                         CartUseCase cartUseCase,
                         OrderPriceProperties orderPriceProperties,
                         GuestPaymentVerificationService guestPaymentVerification,
                         Clock clock) {
        this.productReader = productReader;
        this.inventoryReader = inventoryReader;
        this.cartUseCase = cartUseCase;
        this.orderPriceProperties = orderPriceProperties;
        this.guestPaymentVerification = guestPaymentVerification;
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
            if (op.cartCheckout()) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "장바구니 결제는 회원만 사용할 수 있습니다.");
            }
            if (op.phone() == null || op.verificationCode() == null || op.name() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 주문은 휴대폰 인증이 필요합니다.");
            }
        }

        List<ItemToPrepare> items;
        if (op.cartCheckout()) {
            items = cartItems(auth.userId());
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
        Map<Long, Inventory> inventoriesByProductId = inventoryReader.findByProductIdIn(productIds)
                .stream()
                .collect(toMap(Inventory::getProductId, Function.identity()));
        List<PreparedOrderItem> preparedItems = items.stream()
                .map(item -> prepareItem(item, productsById, inventoriesByProductId))
                .toList();
        MadeToOrderConsent madeToOrderConsent = madeToOrderConsent(op, productsById);
        long total = 0L;
        for (PreparedOrderItem item : preparedItems) {
            total = OrderAmountCalculator.addLine(total, item.qty(), item.unitPrice());
        }
        long shippingFee = op.fulfillmentType() == FulfillmentType.SHIPPING
                ? orderPriceProperties.shippingFee()
                : 0L;
        total = OrderAmountCalculator.addShippingFee(total, shippingFee);

        String phone = auth.isMember() ? null : KoreanPhoneNumber.required(op.phone());
        String name = auth.isMember() ? null : PersonalName.required(op.name());
        String guestVerificationProof = auth.isMember()
                ? null
                : guestPaymentVerification.consumeAndIssue(
                        PaymentContext.ORDER, paymentOrderId, phone, op.verificationCode());
        return new PreparedPayment(total, new PreparedOrderPayload(
                op.userId(), phone, guestVerificationProof, name, preparedItems, op.cartCheckout(),
                op.fulfillmentType(), op.shippingAddress(), shippingFee, madeToOrderConsent));
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

    private List<ItemToPrepare> cartItems(Long userId) {
        return cartUseCase.getPurchasableItems(userId).stream()
                .map(ItemToPrepare::from)
                .toList();
    }

    private List<ItemToPrepare> mergeDirectItems(List<OrderItemRef> requestedItems) {
        Map<Long, Integer> quantitiesByProductId = new LinkedHashMap<>();
        for (OrderItemRef item : requestedItems) {
            if (item == null || item.productId() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 상품이 지정되지 않았습니다.");
            }
            OrderAmountCalculator.requireQuantity(item.qty());
            try {
                int quantity = quantitiesByProductId.merge(item.productId(), item.qty(), Math::addExact);
                OrderAmountCalculator.requireQuantity(quantity);
            } catch (ArithmeticException e) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 수량이 너무 큽니다.");
            }
        }
        return quantitiesByProductId.entrySet().stream()
                .map(entry -> new ItemToPrepare(null, entry.getKey(), entry.getValue()))
                .toList();
    }

    private PreparedOrderItem prepareItem(ItemToPrepare item,
                                          Map<Long, Product> productsById,
                                          Map<Long, Inventory> inventoriesByProductId) {
        Product product = productsById.get(item.productId());
        if (product == null) {
            throw new NotFoundException("상품");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "판매 중인 상품만 주문할 수 있습니다.");
        }
        Inventory inventory = inventoriesByProductId.get(item.productId());
        if (inventory == null) {
            throw new NotFoundException("재고");
        }
        inventory.requireSufficient(item.qty());
        return new PreparedOrderItem(
                item.cartItemId(),
                item.productId(),
                product.getName(),
                item.qty(),
                product.getPrice(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays(),
                product.getType());
    }

    private record ItemToPrepare(Long cartItemId, Long productId, int qty) {

        private static ItemToPrepare from(CartPurchaseItem item) {
            return new ItemToPrepare(item.cartItemId(), item.productId(), item.qty());
        }
    }
}
