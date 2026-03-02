package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.order.OrderApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderApprovalService orderApprovalService;

    public AdminOrderController(OrderApprovalService orderApprovalService) {
        this.orderApprovalService = orderApprovalService;
    }

    /** POST /admin/orders/{id}/approve — 주문 승인 */
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public void approve(@PathVariable Long id) {
        orderApprovalService.approve(id);
    }

    /** POST /admin/orders/{id}/reject — 주문 거절 (환불 + 재고 복구 포함) */
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(@PathVariable Long id) {
        orderApprovalService.reject(id);
    }
}
