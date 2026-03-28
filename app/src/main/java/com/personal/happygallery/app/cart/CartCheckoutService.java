package com.personal.happygallery.app.cart;

import com.personal.happygallery.app.cart.port.in.CartUseCase;
import com.personal.happygallery.app.cart.port.in.CartUseCase.CartItemView;
import com.personal.happygallery.app.cart.port.in.CartUseCase.CartView;
import com.personal.happygallery.app.order.port.in.OrderCreationUseCase;
import com.personal.happygallery.app.order.port.in.OrderCreationUseCase.OrderItemInput;
import com.personal.happygallery.domain.order.Order;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartCheckoutService {

    private final CartUseCase cartUseCase;
    private final OrderCreationUseCase orderCreationUseCase;

    public CartCheckoutService(CartUseCase cartUseCase, OrderCreationUseCase orderCreationUseCase) {
        this.cartUseCase = cartUseCase;
        this.orderCreationUseCase = orderCreationUseCase;
    }

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

        Order order = orderCreationUseCase.createMemberOrder(userId, orderItems);
        cartUseCase.clearCart(userId);
        return order;
    }
}
