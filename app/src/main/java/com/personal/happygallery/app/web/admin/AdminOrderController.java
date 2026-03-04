package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.order.OrderApprovalService;
import com.personal.happygallery.app.order.OrderPickupService;
import com.personal.happygallery.app.order.OrderPickupService.PickupResult;
import com.personal.happygallery.app.order.OrderProductionService;
import com.personal.happygallery.app.order.OrderProductionService.ProductionResult;
import com.personal.happygallery.app.order.PickupExpireBatchService;
import com.personal.happygallery.app.web.admin.dto.MarkPickupReadyRequest;
import com.personal.happygallery.app.web.admin.dto.OrderProductionResponse;
import com.personal.happygallery.app.web.admin.dto.PickupResponse;
import com.personal.happygallery.app.web.admin.dto.SetExpectedShipDateRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderApprovalService orderApprovalService;
    private final OrderProductionService orderProductionService;
    private final OrderPickupService orderPickupService;
    private final PickupExpireBatchService pickupExpireBatchService;

    public AdminOrderController(OrderApprovalService orderApprovalService,
                                OrderProductionService orderProductionService,
                                OrderPickupService orderPickupService,
                                PickupExpireBatchService pickupExpireBatchService) {
        this.orderApprovalService = orderApprovalService;
        this.orderProductionService = orderProductionService;
        this.orderPickupService = orderPickupService;
        this.pickupExpireBatchService = pickupExpireBatchService;
    }

    /** POST /admin/orders/{id}/approve — 주문 승인 (MADE_TO_ORDER는 IN_PRODUCTION으로 전이) */
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public void approve(@PathVariable Long id) {
        orderApprovalService.approve(id);
    }

    /** POST /admin/orders/{id}/reject — 주문 거절 (환불 + 재고 복구 포함, 제작 중은 거절 불가) */
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(@PathVariable Long id) {
        orderApprovalService.reject(id);
    }

    /** PATCH /admin/orders/{id}/expected-ship-date — 예상 출고일 설정/갱신 */
    @PatchMapping("/{id}/expected-ship-date")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse setExpectedShipDate(@PathVariable Long id,
                                                       @RequestBody SetExpectedShipDateRequest request) {
        ProductionResult result = orderProductionService.setExpectedShipDate(id, request.expectedShipDate());
        return new OrderProductionResponse(result.orderId(), result.status(), result.expectedShipDate());
    }

    /** POST /admin/orders/{id}/delay — 고객 동의 후 배송 지연 상태로 전환 */
    @PostMapping("/{id}/delay")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse requestDelay(@PathVariable Long id) {
        ProductionResult result = orderProductionService.requestDelay(id);
        return new OrderProductionResponse(result.orderId(), result.status(), result.expectedShipDate());
    }

    /** POST /admin/orders/{id}/prepare-pickup — 픽업 준비 완료 (APPROVED_FULFILLMENT_PENDING → PICKUP_READY) */
    @PostMapping("/{id}/prepare-pickup")
    @ResponseStatus(HttpStatus.OK)
    public PickupResponse markPickupReady(@PathVariable Long id,
                                         @RequestBody MarkPickupReadyRequest request) {
        PickupResult result = orderPickupService.markPickupReady(id, request.pickupDeadlineAt());
        return new PickupResponse(result.orderId(), result.status(), result.pickupDeadlineAt());
    }

    /** POST /admin/orders/{id}/complete-pickup — 픽업 완료 (PICKUP_READY → PICKED_UP) */
    @PostMapping("/{id}/complete-pickup")
    @ResponseStatus(HttpStatus.OK)
    public PickupResponse confirmPickup(@PathVariable Long id) {
        PickupResult result = orderPickupService.confirmPickup(id);
        return new PickupResponse(result.orderId(), result.status(), result.pickupDeadlineAt());
    }

    /** POST /admin/orders/expire-pickups — 픽업 마감 초과 자동환불 배치 */
    @PostMapping("/expire-pickups")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Integer> expirePickups() {
        BatchResult result = pickupExpireBatchService.expirePickups();
        return Map.of("expiredCount", result.successCount(), "failedCount", result.failureCount());
    }
}
