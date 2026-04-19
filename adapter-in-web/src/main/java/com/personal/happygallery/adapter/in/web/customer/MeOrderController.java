package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.order.port.in.OrderCreationUseCase;
import com.personal.happygallery.application.order.port.in.OrderCreationUseCase.OrderItemInput;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateMemberOrderRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import com.personal.happygallery.domain.order.Order;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/orders")
public class MeOrderController {

    private final OrderQueryUseCase orderQueryUseCase;
    private final OrderCreationUseCase orderCreationUseCase;

    public MeOrderController(OrderQueryUseCase orderQueryUseCase,
                              OrderCreationUseCase orderCreationUseCase) {
        this.orderQueryUseCase = orderQueryUseCase;
        this.orderCreationUseCase = orderCreationUseCase;
    }

    @GetMapping
    public List<MyOrderSummary> myOrders(@CustomerUserId Long userId) {
        return orderQueryUseCase.listMyOrders(userId).stream()
                .map(MyOrderSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderDetailResponse myOrder(@PathVariable Long id, @CustomerUserId Long userId) {
        OrderQueryUseCase.OrderDetail detail = orderQueryUseCase.findMyOrder(id, userId);
        return OrderDetailResponse.from(detail);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary createOrder(@RequestBody @Valid CreateMemberOrderRequest req,
                                      @CustomerUserId Long userId) {
        var items = req.items().stream()
                .map(i -> new OrderItemInput(i.productId(), i.qty()))
                .toList();
        Order order = orderCreationUseCase.createMemberOrder(userId, items);
        return MyOrderSummary.from(order);
    }
}
