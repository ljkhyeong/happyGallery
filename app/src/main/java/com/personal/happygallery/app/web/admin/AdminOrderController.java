package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.app.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.app.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.app.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.app.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.app.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.app.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.app.web.CursorPage;
import com.personal.happygallery.app.web.OffsetPage;
import com.personal.happygallery.app.web.admin.dto.AdminOrderResponse;
import com.personal.happygallery.app.web.admin.dto.BatchResponse;
import com.personal.happygallery.app.web.admin.dto.MarkPickupReadyRequest;
import com.personal.happygallery.app.web.admin.dto.OrderHistoryResponse;
import com.personal.happygallery.app.web.admin.dto.OrderProductionResponse;
import com.personal.happygallery.app.web.admin.dto.PickupResponse;
import com.personal.happygallery.app.web.admin.dto.SetExpectedShipDateRequest;
import com.personal.happygallery.app.web.admin.dto.ShippingResponse;
import com.personal.happygallery.app.web.resolver.AdminUserId;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/orders", "/admin/orders"})
public class AdminOrderController {

    private final AdminOrderQueryUseCase adminOrderQueryUseCase;
    private final AdminOrderSearchUseCase adminOrderSearchUseCase;
    private final OrderApprovalUseCase orderApprovalUseCase;
    private final OrderProductionUseCase orderProductionUseCase;
    private final OrderPickupUseCase orderPickupUseCase;
    private final OrderShippingUseCase orderShippingUseCase;
    private final PickupExpireBatchUseCase pickupExpireBatchUseCase;

    public AdminOrderController(AdminOrderQueryUseCase adminOrderQueryUseCase,
                                AdminOrderSearchUseCase adminOrderSearchUseCase,
                                OrderApprovalUseCase orderApprovalUseCase,
                                OrderProductionUseCase orderProductionUseCase,
                                OrderPickupUseCase orderPickupUseCase,
                                OrderShippingUseCase orderShippingUseCase,
                                PickupExpireBatchUseCase pickupExpireBatchUseCase) {
        this.adminOrderQueryUseCase = adminOrderQueryUseCase;
        this.adminOrderSearchUseCase = adminOrderSearchUseCase;
        this.orderApprovalUseCase = orderApprovalUseCase;
        this.orderProductionUseCase = orderProductionUseCase;
        this.orderPickupUseCase = orderPickupUseCase;
        this.orderShippingUseCase = orderShippingUseCase;
        this.pickupExpireBatchUseCase = pickupExpireBatchUseCase;
    }

    /** GET /admin/orders?status=...&cursor=...&size=20 — 커서 기반 주문 목록 조회 */
    @GetMapping
    public CursorPage<AdminOrderResponse> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderQueryUseCase.listOrders(status, cursor, size);
    }

    /** GET /admin/orders/search — 상태·날짜·키워드 기반 주문 검색 (OFFSET + 지연 조인) */
    @GetMapping("/search")
    public OffsetPage<AdminOrderSearchRow> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderSearchUseCase.search(status, dateFrom, dateTo, keyword, page, size);
    }

    /** POST /admin/orders/{id}/approve — 주문 승인 (MADE_TO_ORDER는 IN_PRODUCTION으로 전이) */
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public void approve(@PathVariable Long id, @AdminUserId Long adminId) {
        orderApprovalUseCase.approve(id, adminId);
    }

    /** POST /admin/orders/{id}/reject — 주문 거절 (환불 + 재고 복구 포함, 제작 중은 거절 불가) */
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(@PathVariable Long id, @AdminUserId Long adminId) {
        orderApprovalUseCase.reject(id, adminId);
    }

    /** POST /admin/orders/{id}/resume-production — 지연 요청에서 제작 재개 (DELAY_REQUESTED → IN_PRODUCTION) */
    @PostMapping("/{id}/resume-production")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse resumeProduction(@PathVariable Long id, @AdminUserId Long adminId) {
        OrderProductionUseCase.ProductionResult result = orderProductionUseCase.resumeProduction(id, adminId);
        return OrderProductionResponse.from(result);
    }

    /** POST /admin/orders/{id}/complete-production — 제작 완료 (IN_PRODUCTION/DELAY_REQUESTED → APPROVED_FULFILLMENT_PENDING) */
    @PostMapping("/{id}/complete-production")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse completeProduction(@PathVariable Long id, @AdminUserId Long adminId) {
        OrderProductionUseCase.ProductionResult result = orderProductionUseCase.completeProduction(id, adminId);
        return OrderProductionResponse.from(result);
    }

    /** PATCH /admin/orders/{id}/expected-ship-date — 예상 출고일 설정/갱신 */
    @PatchMapping("/{id}/expected-ship-date")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse setExpectedShipDate(@PathVariable Long id,
                                                       @RequestBody SetExpectedShipDateRequest request) {
        OrderProductionUseCase.ProductionResult result =
                orderProductionUseCase.setExpectedShipDate(id, request.expectedShipDate());
        return OrderProductionResponse.from(result);
    }

    /** POST /admin/orders/{id}/delay — 고객 동의 후 배송 지연 상태로 전환 */
    @PostMapping("/{id}/delay")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse requestDelay(@PathVariable Long id) {
        OrderProductionUseCase.ProductionResult result = orderProductionUseCase.requestDelay(id);
        return OrderProductionResponse.from(result);
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

    /** GET /admin/orders/{id}/history — 주문 결정 이력 조회 */
    @GetMapping("/{id}/history")
    public List<OrderHistoryResponse> getOrderHistory(@PathVariable Long id) {
        return adminOrderQueryUseCase.getOrderHistory(id);
    }

    /** POST /admin/orders/expire-pickups — 픽업 마감 초과 자동환불 배치 */
    @PostMapping("/expire-pickups")
    @ResponseStatus(HttpStatus.OK)
    public BatchResponse expirePickups() {
        BatchResult result = pickupExpireBatchUseCase.expirePickups();
        return BatchResponse.from(result);
    }

}
