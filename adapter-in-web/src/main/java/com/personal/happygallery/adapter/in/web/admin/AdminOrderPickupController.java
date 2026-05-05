package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.BatchResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.MarkPickupReadyRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.PickupResponse;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/orders", "/admin/orders"})
public class AdminOrderPickupController {

    private final OrderPickupUseCase orderPickupUseCase;
    private final PickupExpireBatchUseCase pickupExpireBatchUseCase;

    public AdminOrderPickupController(OrderPickupUseCase orderPickupUseCase,
                                      PickupExpireBatchUseCase pickupExpireBatchUseCase) {
        this.orderPickupUseCase = orderPickupUseCase;
        this.pickupExpireBatchUseCase = pickupExpireBatchUseCase;
    }

    /** POST /admin/orders/{id}/prepare-pickup — 픽업 준비 완료 (APPROVED_FULFILLMENT_PENDING → PICKUP_READY) */
    @PostMapping("/{id}/prepare-pickup")
    @ResponseStatus(HttpStatus.OK)
    public PickupResponse markPickupReady(@PathVariable Long id,
                                          @RequestBody MarkPickupReadyRequest request) {
        OrderPickupUseCase.PickupResult result = orderPickupUseCase.markPickupReady(id, request.pickupDeadlineAt());
        return PickupResponse.from(result);
    }

    /** POST /admin/orders/{id}/complete-pickup — 픽업 완료 (PICKUP_READY → PICKED_UP) */
    @PostMapping("/{id}/complete-pickup")
    @ResponseStatus(HttpStatus.OK)
    public PickupResponse confirmPickup(@PathVariable Long id) {
        OrderPickupUseCase.PickupResult result = orderPickupUseCase.confirmPickup(id);
        return PickupResponse.from(result);
    }

    /** POST /admin/orders/expire-pickups — 픽업 마감 초과 자동환불 배치 */
    @PostMapping("/expire-pickups")
    @ResponseStatus(HttpStatus.OK)
    public BatchResponse expirePickups() {
        BatchResult result = pickupExpireBatchUseCase.expirePickups();
        return BatchResponse.from(result);
    }
}
