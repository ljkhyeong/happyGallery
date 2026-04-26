package com.personal.happygallery.adapter.in.web.order;

import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase.OrderDetail;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 조회 API.
 *
 * <p>주문 생성은 {@code POST /api/v1/payments/prepare} → {@code /confirm} 경로로 일원화됨.
 * 이 컨트롤러는 비회원 토큰 기반 조회만 담당한다.
 */
@RestController
@RequestMapping({"/api/v1/orders", "/orders"})
public class OrderController {

    private final OrderQueryUseCase orderQueryUseCase;

    public OrderController(OrderQueryUseCase orderQueryUseCase) {
        this.orderQueryUseCase = orderQueryUseCase;
    }

    /** GET /orders/{id} — 주문 상세 조회 (X-Access-Token 헤더) */
    @GetMapping("/{id}")
    public OrderDetailResponse getOrder(@PathVariable Long id,
                                         @RequestHeader("X-Access-Token") String token) {
        OrderDetail detail = orderQueryUseCase.getOrderByToken(id, token);
        return OrderDetailResponse.from(detail);
    }
}
