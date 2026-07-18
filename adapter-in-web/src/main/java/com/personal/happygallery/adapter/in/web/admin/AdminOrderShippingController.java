package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.ShippingResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderShippingController {

    private final OrderShippingUseCase orderShippingUseCase;

    public AdminOrderShippingController(OrderShippingUseCase orderShippingUseCase) {
        this.orderShippingUseCase = orderShippingUseCase;
    }

    /** POST /api/v1/admin/orders/{id}/prepare-shipping — 배송 준비 (APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING) */
    @PostMapping("/{id}/prepare-shipping")
    public ShippingResponse prepareShipping(@PathVariable Long id,
                                            @AuthenticationPrincipal AdminPrincipal admin) {
        OrderShippingUseCase.ShippingResult result = orderShippingUseCase.prepareShipping(
                id, admin.adminUserId());
        return ShippingResponse.from(result);
    }

    /** POST /api/v1/admin/orders/{id}/mark-shipped — 배송 출발 (SHIPPING_PREPARING → SHIPPED) */
    @PostMapping("/{id}/mark-shipped")
    public ShippingResponse markShipped(@PathVariable Long id,
                                        @AuthenticationPrincipal AdminPrincipal admin) {
        OrderShippingUseCase.ShippingResult result = orderShippingUseCase.markShipped(
                id, admin.adminUserId());
        return ShippingResponse.from(result);
    }

    /** POST /api/v1/admin/orders/{id}/mark-delivered — 배송 완료 (SHIPPED → DELIVERED) */
    @PostMapping("/{id}/mark-delivered")
    public ShippingResponse markDelivered(@PathVariable Long id,
                                          @AuthenticationPrincipal AdminPrincipal admin) {
        OrderShippingUseCase.ShippingResult result = orderShippingUseCase.markDelivered(
                id, admin.adminUserId());
        return ShippingResponse.from(result);
    }
}
