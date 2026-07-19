package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartCheckoutUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartItemView;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartView;
import com.personal.happygallery.application.order.OrderCreationService;
import com.personal.happygallery.application.order.OrderCreationService.OrderItemInput;
import com.personal.happygallery.domain.order.Order;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCartCheckoutService implements CartCheckoutUseCase {

    private final CartUseCase cartUseCase;
    private final OrderCreationService orderCreationService;

    public DefaultCartCheckoutService(CartUseCase cartUseCase, OrderCreationService orderCreationService) {
        this.cartUseCase = cartUseCase;
        this.orderCreationService = orderCreationService;
    }

    @Override
    public Order checkout(Long userId) {
        CartView cart = cartUseCase.getCart(userId);
        if (cart.items().isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있습니다.");
        }

        List<OrderItemInput> orderItems = cart.items().stream()
                .filter(CartItemView::available)
                .map(item -> new OrderItemInput(item.productId(), item.qty()))
                .toList();

        if (orderItems.isEmpty()) {
            throw new IllegalStateException("구매 가능한 상품이 없습니다.");
        }

        Order order = orderCreationService.createMemberOrder(userId, orderItems);
        cartUseCase.clearCart(userId);
        return order;
    }
}
