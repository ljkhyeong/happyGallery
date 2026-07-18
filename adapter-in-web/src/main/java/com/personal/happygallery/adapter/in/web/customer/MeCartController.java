package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.cart.port.in.CartCheckoutUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartView;
import com.personal.happygallery.adapter.in.web.customer.dto.AddCartItemRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CartResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.adapter.in.web.customer.dto.UpdateCartItemRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.domain.order.Order;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final SubjectRateLimitGuard rateLimitGuard;

    public MeCartController(CartUseCase cartUseCase,
                            CartCheckoutUseCase cartCheckoutUseCase,
                            SubjectRateLimitGuard rateLimitGuard) {
        this.cartUseCase = cartUseCase;
        this.cartCheckoutUseCase = cartCheckoutUseCase;
        this.rateLimitGuard = rateLimitGuard;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal CustomerPrincipal customer) {
        CartView cart = cartUseCase.getCart(customer.userId());
        return CartResponse.from(cart);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public void addItem(@RequestBody @Valid AddCartItemRequest req,
                        @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.addItem(customer.userId(), req.productId(), req.qty());
    }

    @PutMapping("/items/{productId}")
    public void updateItemQty(@PathVariable Long productId,
                              @RequestBody @Valid UpdateCartItemRequest req,
                              @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.updateItemQty(customer.userId(), productId, req.qty());
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long productId,
                           @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.removeItem(customer.userId(), productId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary checkout(@AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkCartCheckout(customer.userId());
        Order order = cartCheckoutUseCase.checkout(customer.userId());
        return MyOrderSummary.from(order);
    }
}
