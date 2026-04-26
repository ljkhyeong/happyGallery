package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.OrderService.OrderItemRequest;
import com.personal.happygallery.application.order.port.in.OrderCreationUseCase.OrderCreationResult;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderFulfiller implements PaymentFulfiller {

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final ProductReaderPort productReader;
    private final OrderService orderService;

    public OrderFulfiller(VerifiedGuestResolver verifiedGuestResolver,
                          ProductReaderPort productReader,
                          OrderService orderService) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.productReader = productReader;
        this.orderService = orderService;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PaymentPayload payload, AuthContext auth) {
        if (!(payload instanceof OrderPayload op)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 결제 payload가 아닙니다.");
        }

        List<OrderItemRequest> orderItems = resolveItemPrices(op.items());

        if (auth.isMember()) {
            if (op.userId() == null || !op.userId().equals(auth.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "회원 정보가 인증과 일치하지 않습니다.");
            }
            Order order = orderService.createMemberOrder(auth.userId(), orderItems);
            return new FulfillResult(order.getId(), null);
        }

        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(op.phone(), op.verificationCode(), op.name());
        OrderCreationResult result = orderService.createPaidOrder(guest.getId(), orderItems);
        return new FulfillResult(result.order().getId(), result.rawAccessToken());
    }

    private List<OrderItemRequest> resolveItemPrices(List<OrderItemRef> items) {
        List<OrderItemRequest> resolved = new ArrayList<>(items.size());
        for (OrderItemRef item : items) {
            Product product = productReader.findById(item.productId())
                    .orElseThrow(NotFoundException.supplier("상품"));
            resolved.add(new OrderItemRequest(item.productId(), item.qty(), product.getPrice()));
        }
        return resolved;
    }
}
