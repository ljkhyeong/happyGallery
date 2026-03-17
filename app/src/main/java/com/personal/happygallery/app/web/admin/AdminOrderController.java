package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.order.OrderApprovalService;
import com.personal.happygallery.app.order.OrderPickupService;
import com.personal.happygallery.app.order.OrderProductionService;
import com.personal.happygallery.app.order.OrderShippingService;
import com.personal.happygallery.app.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.app.web.admin.dto.AdminOrderResponse;
import com.personal.happygallery.app.web.admin.dto.BatchResponse;
import com.personal.happygallery.app.web.admin.dto.MarkPickupReadyRequest;
import com.personal.happygallery.app.web.admin.dto.OrderHistoryResponse;
import com.personal.happygallery.app.web.admin.dto.OrderProductionResponse;
import com.personal.happygallery.app.web.admin.dto.PickupResponse;
import com.personal.happygallery.app.web.admin.dto.SetExpectedShipDateRequest;
import com.personal.happygallery.app.web.admin.dto.ShippingResponse;
import com.personal.happygallery.app.web.AdminAuthFilter;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.domain.order.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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

    private final OrderApprovalService orderApprovalService;
    private final OrderProductionService orderProductionService;
    private final OrderPickupService orderPickupService;
    private final OrderShippingService orderShippingService;
    private final PickupExpireBatchUseCase pickupExpireBatchService;
    private final OrderReaderPort orderReader;
    private final OrderHistoryPort orderHistoryPort;

    public AdminOrderController(OrderApprovalService orderApprovalService,
                                OrderProductionService orderProductionService,
                                OrderPickupService orderPickupService,
                                OrderShippingService orderShippingService,
                                PickupExpireBatchUseCase pickupExpireBatchService,
                                OrderReaderPort orderReader,
                                OrderHistoryPort orderHistoryPort) {
        this.orderApprovalService = orderApprovalService;
        this.orderProductionService = orderProductionService;
        this.orderPickupService = orderPickupService;
        this.orderShippingService = orderShippingService;
        this.pickupExpireBatchService = pickupExpireBatchService;
        this.orderReader = orderReader;
        this.orderHistoryPort = orderHistoryPort;
    }

    /** GET /admin/orders?status=PAID_APPROVAL_PENDING — 상태별 주문 목록 조회 (상태 미지정 시 전체) */
    @GetMapping
    public List<AdminOrderResponse> listOrders(
            @RequestParam(required = false) OrderStatus status) {
        var orders = (status != null)
                ? orderReader.findByStatusOrderByCreatedAtDesc(status)
                : orderReader.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(AdminOrderResponse::from).toList();
    }

    /** POST /admin/orders/{id}/approve — 주문 승인 (MADE_TO_ORDER는 IN_PRODUCTION으로 전이) */
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public void approve(@PathVariable Long id, HttpServletRequest request) {
        orderApprovalService.approve(id, adminId(request));
    }

    /** POST /admin/orders/{id}/reject — 주문 거절 (환불 + 재고 복구 포함, 제작 중은 거절 불가) */
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(@PathVariable Long id, HttpServletRequest request) {
        orderApprovalService.reject(id, adminId(request));
    }

    /** POST /admin/orders/{id}/resume-production — 지연 요청에서 제작 재개 (DELAY_REQUESTED → IN_PRODUCTION) */
    @PostMapping("/{id}/resume-production")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse resumeProduction(@PathVariable Long id, HttpServletRequest request) {
        return OrderProductionResponse.from(orderProductionService.resumeProduction(id, adminId(request)));
    }

    /** POST /admin/orders/{id}/complete-production — 제작 완료 (IN_PRODUCTION/DELAY_REQUESTED → APPROVED_FULFILLMENT_PENDING) */
    @PostMapping("/{id}/complete-production")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse completeProduction(@PathVariable Long id, HttpServletRequest request) {
        return OrderProductionResponse.from(orderProductionService.completeProduction(id, adminId(request)));
    }

    /** PATCH /admin/orders/{id}/expected-ship-date — 예상 출고일 설정/갱신 */
    @PatchMapping("/{id}/expected-ship-date")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse setExpectedShipDate(@PathVariable Long id,
                                                       @RequestBody SetExpectedShipDateRequest request) {
        return OrderProductionResponse.from(orderProductionService.setExpectedShipDate(id, request.expectedShipDate()));
    }

    /** POST /admin/orders/{id}/delay — 고객 동의 후 배송 지연 상태로 전환 */
    @PostMapping("/{id}/delay")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse requestDelay(@PathVariable Long id) {
        return OrderProductionResponse.from(orderProductionService.requestDelay(id));
    }

    /** POST /admin/orders/{id}/prepare-pickup — 픽업 준비 완료 (APPROVED_FULFILLMENT_PENDING → PICKUP_READY) */
    @PostMapping("/{id}/prepare-pickup")
    @ResponseStatus(HttpStatus.OK)
    public PickupResponse markPickupReady(@PathVariable Long id,
                                         @RequestBody MarkPickupReadyRequest request) {
        return PickupResponse.from(orderPickupService.markPickupReady(id, request.pickupDeadlineAt()));
    }

    /** POST /admin/orders/{id}/complete-pickup — 픽업 완료 (PICKUP_READY → PICKED_UP) */
    @PostMapping("/{id}/complete-pickup")
    @ResponseStatus(HttpStatus.OK)
    public PickupResponse confirmPickup(@PathVariable Long id) {
        return PickupResponse.from(orderPickupService.confirmPickup(id));
    }

    /** POST /admin/orders/{id}/prepare-shipping — 배송 준비 (APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING) */
    @PostMapping("/{id}/prepare-shipping")
    @ResponseStatus(HttpStatus.OK)
    public ShippingResponse prepareShipping(@PathVariable Long id, HttpServletRequest request) {
        return ShippingResponse.from(orderShippingService.prepareShipping(id, adminId(request)));
    }

    /** POST /admin/orders/{id}/mark-shipped — 배송 출발 (SHIPPING_PREPARING → SHIPPED) */
    @PostMapping("/{id}/mark-shipped")
    @ResponseStatus(HttpStatus.OK)
    public ShippingResponse markShipped(@PathVariable Long id, HttpServletRequest request) {
        return ShippingResponse.from(orderShippingService.markShipped(id, adminId(request)));
    }

    /** POST /admin/orders/{id}/mark-delivered — 배송 완료 (SHIPPED → DELIVERED) */
    @PostMapping("/{id}/mark-delivered")
    @ResponseStatus(HttpStatus.OK)
    public ShippingResponse markDelivered(@PathVariable Long id, HttpServletRequest request) {
        return ShippingResponse.from(orderShippingService.markDelivered(id, adminId(request)));
    }

    /** GET /admin/orders/{id}/history — 주문 결정 이력 조회 */
    @GetMapping("/{id}/history")
    public List<OrderHistoryResponse> getOrderHistory(@PathVariable Long id) {
        return orderHistoryPort.findByOrderIdOrderByDecidedAtAsc(id).stream()
                .map(OrderHistoryResponse::from)
                .toList();
    }

    /** POST /admin/orders/expire-pickups — 픽업 마감 초과 자동환불 배치 */
    @PostMapping("/expire-pickups")
    @ResponseStatus(HttpStatus.OK)
    public BatchResponse expirePickups() {
        BatchResult result = pickupExpireBatchService.expirePickups();
        return BatchResponse.from(result);
    }

    /** Bearer 세션에서 검증된 admin user ID를 추출한다. API Key 폴백 시 null. */
    private static Long adminId(HttpServletRequest request) {
        return (Long) request.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR);
    }
}
