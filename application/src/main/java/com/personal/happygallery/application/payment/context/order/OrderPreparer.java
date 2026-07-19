package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartPurchaseItem;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static java.util.stream.Collectors.toMap;

@Component
public class OrderPreparer implements PaymentPreparer {

    private final ProductReaderPort productReader;
    private final CartUseCase cartUseCase;

    public OrderPreparer(ProductReaderPort productReader, CartUseCase cartUseCase) {
        this.productReader = productReader;
        this.cartUseCase = cartUseCase;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    public PreparedPayment prepare(PaymentPayload payload, AuthContext auth) {
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
            items = op.items().stream().map(ItemToPrepare::from).toList();
        }
        if (items.isEmpty()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 항목이 비었습니다.");
        }
        for (ItemToPrepare item : items) {
            if (item == null || item.productId() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 상품이 지정되지 않았습니다.");
            }
            if (item.qty() <= 0) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 수량은 1 이상이어야 합니다.");
            }
        }

        Map<Long, Product> productsById = productReader.findAllById(items.stream()
                        .map(ItemToPrepare::productId)
                        .distinct()
                        .toList())
                .stream()
                .collect(toMap(Product::getId, Function.identity()));
        List<PreparedOrderItem> preparedItems = items.stream()
                .map(item -> prepareItem(item, productsById))
                .toList();
        long total = preparedItems.stream()
                .mapToLong(item -> (long) item.qty() * item.unitPrice())
                .sum();

        String phone = auth.isMember() ? null : KoreanPhoneNumber.required(op.phone());
        String name = auth.isMember() ? null : PersonalName.required(op.name());
        return new PreparedPayment(total, new PreparedOrderPayload(
                op.userId(), phone, op.verificationCode(), name, preparedItems, op.cartCheckout(),
                op.fulfillmentType(), op.shippingAddress()));
    }

    private List<ItemToPrepare> cartItems(Long userId) {
        return cartUseCase.getPurchasableItems(userId).stream()
                .map(ItemToPrepare::from)
                .toList();
    }

    private PreparedOrderItem prepareItem(ItemToPrepare item, Map<Long, Product> productsById) {
        Product product = productsById.get(item.productId());
        if (product == null) {
            throw new NotFoundException("상품");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "판매 중인 상품만 주문할 수 있습니다.");
        }
        return new PreparedOrderItem(item.cartItemId(), item.productId(), item.qty(), product.getPrice());
    }

    private record ItemToPrepare(Long cartItemId, Long productId, int qty) {

        private static ItemToPrepare from(OrderItemRef item) {
            return item == null ? new ItemToPrepare(null, null, 0)
                    : new ItemToPrepare(null, item.productId(), item.qty());
        }

        private static ItemToPrepare from(CartPurchaseItem item) {
            return new ItemToPrepare(item.cartItemId(), item.productId(), item.qty());
        }
    }
}
