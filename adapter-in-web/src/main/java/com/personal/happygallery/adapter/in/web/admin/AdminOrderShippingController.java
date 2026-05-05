package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.ShippingResponse;
import com.personal.happygallery.adapter.in.web.resolver.AdminUserId;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/orders", "/admin/orders"})
public class AdminOrderShippingController {

    private final OrderShippingUseCase orderShippingUseCase;

    public AdminOrderShippingController(OrderShippingUseCase orderShippingUseCase) {
        this.orderShippingUseCase = orderShippingUseCase;
    }

    /** POST /admin/orders/{id}/prepare-shipping — 배송 준비 (APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING) */
    @PostMapping("/{id}/prepare-shipping")
    @ResponseStatus(HttpStatus.OK)
    public ShippingResponse prepareShipping(@PathVariable Long id, @AdminUserId Long adminId) {
        OrderShippingUseCase.ShippingResult result = orderShippingUseCase.prepareShipping(id, adminId);
        return ShippingResponse.from(result);
    }

    /** POST /admin/orders/{id}/mark-shipped — 배송 출발 (SHIPPING_PREPARING → SHIPPED) */
    @PostMapping("/{id}/mark-shipped")
    @ResponseStatus(HttpStatus.OK)
    public ShippingResponse markShipped(@PathVariable Long id, @AdminUserId Long adminId) {
        OrderShippingUseCase.ShippingResult result = orderShippingUseCase.markShipped(id, adminId);
        return ShippingResponse.from(result);
    }

    /** POST /admin/orders/{id}/mark-delivered — 배송 완료 (SHIPPED → DELIVERED) */
    @PostMapping("/{id}/mark-delivered")
    @ResponseStatus(HttpStatus.OK)
    public ShippingResponse markDelivered(@PathVariable Long id, @AdminUserId Long adminId) {
        OrderShippingUseCase.ShippingResult result = orderShippingUseCase.markDelivered(id, adminId);
        return ShippingResponse.from(result);
    }
}
