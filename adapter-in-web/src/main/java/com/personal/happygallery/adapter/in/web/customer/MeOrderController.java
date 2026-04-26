package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 주문 조회 API.
 *
 * <p>주문 생성은 {@code POST /api/v1/payments/prepare} → {@code /confirm} 경로로 일원화됨.
 */
@RestController
@RequestMapping("/api/v1/me/orders")
public class MeOrderController {

    private final OrderQueryUseCase orderQueryUseCase;

    public MeOrderController(OrderQueryUseCase orderQueryUseCase) {
        this.orderQueryUseCase = orderQueryUseCase;
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
}
