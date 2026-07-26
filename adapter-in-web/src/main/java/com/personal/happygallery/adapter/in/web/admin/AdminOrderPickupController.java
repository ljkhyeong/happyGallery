package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.BatchResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.MarkPickupReadyRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.PickupResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderPickupController {

    private final OrderPickupUseCase orderPickupUseCase;
    private final PickupExpireBatchUseCase pickupExpireBatchUseCase;

    public AdminOrderPickupController(OrderPickupUseCase orderPickupUseCase,
                                      PickupExpireBatchUseCase pickupExpireBatchUseCase) {
        this.orderPickupUseCase = orderPickupUseCase;
        this.pickupExpireBatchUseCase = pickupExpireBatchUseCase;
    }

    /** POST /api/v1/admin/orders/{id}/prepare-pickup — 픽업 준비 완료 (APPROVED_FULFILLMENT_PENDING → PICKUP_READY) */
    @PostMapping("/{id}/prepare-pickup")
    public PickupResponse markPickupReady(@PathVariable Long id,
                                          @RequestBody MarkPickupReadyRequest request,
                                          @AuthenticationPrincipal AdminPrincipal admin) {
        OrderPickupUseCase.PickupResult result = orderPickupUseCase.markPickupReady(
                id, request.pickupDeadlineAt(), admin.auditActorId());
        return PickupResponse.from(result);
    }

    /** POST /api/v1/admin/orders/{id}/complete-pickup — 픽업 완료 (PICKUP_READY → PICKED_UP) */
    @PostMapping("/{id}/complete-pickup")
    public PickupResponse confirmPickup(@PathVariable Long id,
                                        @AuthenticationPrincipal AdminPrincipal admin) {
        OrderPickupUseCase.PickupResult result = orderPickupUseCase.confirmPickup(
                id, admin.auditActorId());
        return PickupResponse.from(result);
    }

    /** POST /api/v1/admin/orders/expire-pickups — 픽업 마감 초과 처리 배치 */
    @PostMapping("/expire-pickups")
    public BatchResponse expirePickups() {
        BatchResult result = pickupExpireBatchUseCase.expirePickups();
        return BatchResponse.from(result);
    }
}
