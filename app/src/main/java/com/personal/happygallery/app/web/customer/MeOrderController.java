package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.order.OrderQueryService;
import com.personal.happygallery.app.order.OrderService;
import com.personal.happygallery.app.product.ProductQueryService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.domain.order.Order;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
    private final OrderService orderService;
    private final ProductQueryService productQueryService;

    public MeOrderController(OrderQueryService orderQueryService,
                              OrderService orderService,
                              ProductQueryService productQueryService) {
        this.orderQueryService = orderQueryService;
        this.orderService = orderService;
        this.productQueryService = productQueryService;
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
        OrderQueryService.OrderDetail detail = orderQueryService.findMyOrder(id, userId);
        return OrderDetailResponse.from(detail.order(), detail.items(), detail.fulfillment());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary createOrder(@RequestBody @Valid CreateMemberOrderRequest req,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        List<OrderService.OrderItemRequest> items = req.items().stream()
                .map(i -> {
                    var product = productQueryService.getProduct(i.productId()).product();
                    return new OrderService.OrderItemRequest(i.productId(), i.qty(), product.getPrice());
                })
                .toList();
        Order order = orderService.createMemberOrder(userId, items);
        return MyOrderSummary.from(order);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }

    // ── DTO ──

    public record MyOrderSummary(Long orderId, String status, long totalAmount,
                                  LocalDateTime paidAt, LocalDateTime createdAt) {
        static MyOrderSummary from(Order o) {
            return new MyOrderSummary(o.getId(), o.getStatus().name(),
                    o.getTotalAmount(), o.getPaidAt(), o.getCreatedAt());
        }
    }

    public record CreateMemberOrderRequest(
            @NotEmpty List<OrderItemDto> items) {}

    public record OrderItemDto(
            @NotNull Long productId,
            @Min(1) int qty) {}
}
