package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.cart.port.in.CartCheckoutUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartView;
import com.personal.happygallery.adapter.in.web.customer.dto.AddCartItemRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CartResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.adapter.in.web.customer.dto.UpdateCartItemRequest;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import com.personal.happygallery.domain.order.Order;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/cart")
public class MeCartController {

    private final CartUseCase cartUseCase;
    private final CartCheckoutUseCase cartCheckoutUseCase;

    public MeCartController(CartUseCase cartUseCase, CartCheckoutUseCase cartCheckoutUseCase) {
        this.cartUseCase = cartUseCase;
        this.cartCheckoutUseCase = cartCheckoutUseCase;
    }

    @GetMapping
    public CartResponse getCart(@CustomerUserId Long userId) {
        CartView cart = cartUseCase.getCart(userId);
        return CartResponse.from(cart);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public void addItem(@RequestBody @Valid AddCartItemRequest req, @CustomerUserId Long userId) {
        cartUseCase.addItem(userId, req.productId(), req.qty());
    }

    @PutMapping("/items/{productId}")
    public void updateItemQty(@PathVariable Long productId,
                              @RequestBody @Valid UpdateCartItemRequest req,
                              @CustomerUserId Long userId) {
        cartUseCase.updateItemQty(userId, productId, req.qty());
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long productId, @CustomerUserId Long userId) {
        cartUseCase.removeItem(userId, productId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary checkout(@CustomerUserId Long userId) {
        Order order = cartCheckoutUseCase.checkout(userId);
        return MyOrderSummary.from(order);
    }
}
