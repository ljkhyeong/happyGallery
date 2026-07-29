package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartView;
import com.personal.happygallery.application.cart.port.in.CartUseCase.MergeItem;
import com.personal.happygallery.adapter.in.web.customer.dto.AddCartItemRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CartResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MergeCartRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.UpdateCartItemRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
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

    public MeCartController(CartUseCase cartUseCase) {
        this.cartUseCase = cartUseCase;
    }

    @GetMapping
    @Operation(operationId = "getMyCart")
    public CartResponse getCart(@AuthenticationPrincipal CustomerPrincipal customer) {
        CartView cart = cartUseCase.getCart(customer.userId());
        return CartResponse.from(cart);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "addMyCartItem")
    public void addItem(@RequestBody @Valid AddCartItemRequest req,
                        @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.addItem(customer.userId(), req.productId(), req.qty());
    }

    @PostMapping("/merge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "mergeMyCartItems")
    public void mergeItems(@RequestBody @Valid MergeCartRequest request,
                           @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.mergeItems(
                customer.userId(),
                request.idempotencyKey(),
                request.items().stream()
                        .map(item -> new MergeItem(item.productId(), item.qty()))
                        .toList());
    }

    @PutMapping("/items/{productId}")
    @Operation(operationId = "updateMyCartItemQuantity")
    public void updateItemQty(@PathVariable Long productId,
                              @RequestBody @Valid UpdateCartItemRequest req,
                              @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.updateItemQty(customer.userId(), productId, req.qty());
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "removeMyCartItem")
    public void removeItem(@PathVariable Long productId,
                           @AuthenticationPrincipal CustomerPrincipal customer) {
        cartUseCase.removeItem(customer.userId(), productId);
    }
}
