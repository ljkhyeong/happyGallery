package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.OrderCreationUseCase;
import com.personal.happygallery.app.customer.VerifiedGuestResolver;
import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.error.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공개 주문 생성 — 휴대폰 인증 기반.
 */
@Service
@Transactional
public class DefaultOrderCreationService implements OrderCreationUseCase {

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final ProductReaderPort productReader;
    private final OrderService orderService;

    public DefaultOrderCreationService(VerifiedGuestResolver verifiedGuestResolver,
                                       ProductReaderPort productReader,
                                       OrderService orderService) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.productReader = productReader;
        this.orderService = orderService;
    }

    /**
     * 휴대폰 인증 기반 주문 생성.
     */
    public OrderCreationResult createOrderByPhone(CreateOrderByPhoneCommand command) {
        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(
                command.phone(),
                command.verificationCode(),
                command.name());
        List<OrderService.OrderItemRequest> orderItems = resolveItemPrices(command.items());
        return orderService.createPaidOrder(guest.getId(), orderItems);
    }

    /**
     * 회원 주문 생성 — 세션 userId 기반.
     */
    public Order createMemberOrder(Long userId, List<OrderItemInput> items) {
        List<OrderService.OrderItemRequest> orderItems = resolveItemPrices(items);
        return orderService.createMemberOrder(userId, orderItems);
    }

    private List<OrderService.OrderItemRequest> resolveItemPrices(List<OrderItemInput> items) {
        return items.stream()
                .map(item -> {
                    Product product = productReader.findById(item.productId())
                            .orElseThrow(() -> new NotFoundException("상품"));
                    return new OrderService.OrderItemRequest(
                            item.productId(), item.qty(), product.getPrice());
                })
                .toList();
    }
}
