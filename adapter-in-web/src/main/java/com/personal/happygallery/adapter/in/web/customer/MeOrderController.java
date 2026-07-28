package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.MyOrderSummary;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(operationId = "listMyOrders")
    public List<MyOrderSummary> myOrders(@AuthenticationPrincipal CustomerPrincipal customer) {
        return MyOrderSummary.fromAll(orderQueryUseCase.listMyOrders(customer.userId()));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMyOrder")
    public OrderDetailResponse myOrder(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomerPrincipal customer) {
        OrderQueryUseCase.OrderDetail detail = orderQueryUseCase.findMyOrder(id, customer.userId());
        return OrderDetailResponse.from(detail);
    }
}
