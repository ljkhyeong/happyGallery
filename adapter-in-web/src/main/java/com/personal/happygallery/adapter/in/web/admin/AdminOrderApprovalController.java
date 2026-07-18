package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.OrderRejectResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderApprovalController {

    private final OrderApprovalUseCase orderApprovalUseCase;

    public AdminOrderApprovalController(OrderApprovalUseCase orderApprovalUseCase) {
        this.orderApprovalUseCase = orderApprovalUseCase;
    }

    /** POST /api/v1/admin/orders/{id}/approve — 주문 승인 (MADE_TO_ORDER는 IN_PRODUCTION으로 전이) */
    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id,
                        @AuthenticationPrincipal AdminPrincipal admin) {
        orderApprovalUseCase.approve(id, admin.adminUserId());
    }

    /** POST /api/v1/admin/orders/{id}/reject — 주문 거절 (환불 + 재고 복구 포함, 제작 중은 거절 불가) */
    @PostMapping("/{id}/reject")
    public OrderRejectResponse reject(@PathVariable Long id,
                                      @AuthenticationPrincipal AdminPrincipal admin) {
        return OrderRejectResponse.from(orderApprovalUseCase.reject(id, admin.adminUserId()));
    }
}
