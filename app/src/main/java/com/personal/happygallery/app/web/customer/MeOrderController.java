package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.order.OrderCreationService;
import com.personal.happygallery.app.order.OrderQueryService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CreateMemberOrderRequest;
import com.personal.happygallery.app.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.app.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.domain.order.Order;
import jakarta.servlet.http.HttpServletRequest;
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

    private final OrderQueryService orderQueryService;
    private final OrderCreationService orderCreationService;

    public MeOrderController(OrderQueryService orderQueryService,
                              OrderCreationService orderCreationService) {
        this.orderQueryService = orderQueryService;
        this.orderCreationService = orderCreationService;
    }

    @GetMapping
    public List<MyOrderSummary> myOrders(HttpServletRequest request) {
        Long userId = getUserId(request);
        return orderQueryService.listMyOrders(userId).stream()
                .map(MyOrderSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderDetailResponse myOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return OrderDetailResponse.from(orderQueryService.findMyOrder(id, userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary createOrder(@RequestBody @Valid CreateMemberOrderRequest req,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        var items = req.items().stream()
                .map(i -> new OrderCreationService.OrderItemInput(i.productId(), i.qty()))
                .toList();
        Order order = orderCreationService.createMemberOrder(userId, items);
        return MyOrderSummary.from(order);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
