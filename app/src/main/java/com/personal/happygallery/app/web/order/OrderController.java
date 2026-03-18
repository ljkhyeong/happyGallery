package com.personal.happygallery.app.web.order;

import com.personal.happygallery.app.order.OrderCreationService;
import com.personal.happygallery.app.order.OrderQueryService;
import com.personal.happygallery.app.web.order.dto.CreateOrderRequest;
import com.personal.happygallery.app.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.app.web.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/orders", "/orders"})
public class OrderController {

    private final OrderCreationService orderCreationService;
    private final OrderQueryService orderQueryService;

    public OrderController(OrderCreationService orderCreationService,
                           OrderQueryService orderQueryService) {
        this.orderCreationService = orderCreationService;
        this.orderQueryService = orderQueryService;
    }

    /** POST /orders — 주문 생성 (휴대폰 인증 기반) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        var items = request.items().stream()
                .map(i -> new OrderCreationService.OrderItemInput(i.productId(), i.qty()))
                .toList();
        var result = orderCreationService.createOrderByPhone(
                request.phone(), request.verificationCode(), request.name(), items);
        return OrderResponse.from(result.order(), result.rawAccessToken());
    }

    /** GET /orders/{id} — 주문 상세 조회 (X-Access-Token 헤더) */
    @GetMapping("/{id}")
    public OrderDetailResponse getOrder(@PathVariable Long id,
                                         @RequestHeader("X-Access-Token") String token) {
        OrderQueryService.OrderDetail detail = orderQueryService.getOrderByToken(id, token);
        return OrderDetailResponse.from(detail);
    }
}
