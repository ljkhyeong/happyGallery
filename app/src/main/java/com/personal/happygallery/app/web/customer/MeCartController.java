package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.cart.CartCheckoutService;
import com.personal.happygallery.app.cart.port.in.CartUseCase;
import com.personal.happygallery.app.cart.port.in.CartUseCase.CartView;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.AddCartItemRequest;
import com.personal.happygallery.app.web.customer.dto.CartResponse;
import com.personal.happygallery.app.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.app.web.customer.dto.UpdateCartItemRequest;
import com.personal.happygallery.domain.order.Order;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CartCheckoutService cartCheckoutService;

    public MeCartController(CartUseCase cartUseCase, CartCheckoutService cartCheckoutService) {
        this.cartUseCase = cartUseCase;
        this.cartCheckoutService = cartCheckoutService;
    }

    @GetMapping
    public CartResponse getCart(HttpServletRequest request) {
        CartView cart = cartUseCase.getCart(getUserId(request));
        return CartResponse.from(cart);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public void addItem(@RequestBody @Valid AddCartItemRequest req, HttpServletRequest request) {
        cartUseCase.addItem(getUserId(request), req.productId(), req.qty());
    }

    @PutMapping("/items/{productId}")
    public void updateItemQty(@PathVariable Long productId,
                              @RequestBody @Valid UpdateCartItemRequest req,
                              HttpServletRequest request) {
        cartUseCase.updateItemQty(getUserId(request), productId, req.qty());
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long productId, HttpServletRequest request) {
        cartUseCase.removeItem(getUserId(request), productId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary checkout(HttpServletRequest request) {
        Order order = cartCheckoutService.checkout(getUserId(request));
        return MyOrderSummary.from(order);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
