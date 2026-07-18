package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.OrderProductionResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.OrderDelayCancellationResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SetExpectedShipDateRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderProductionController {

    private final OrderProductionUseCase orderProductionUseCase;

    public AdminOrderProductionController(OrderProductionUseCase orderProductionUseCase) {
        this.orderProductionUseCase = orderProductionUseCase;
    }

    /** POST /api/v1/admin/orders/{id}/resume-production — 지연 요청에서 제작 재개 (DELAY_REQUESTED → IN_PRODUCTION) */
    @PostMapping("/{id}/resume-production")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse resumeProduction(@PathVariable Long id,
                                                    @AuthenticationPrincipal AdminPrincipal admin) {
        OrderProductionUseCase.ProductionResult result = orderProductionUseCase.resumeProduction(
                id, admin.adminUserId());
        return OrderProductionResponse.from(result);
    }

    /** POST /api/v1/admin/orders/{id}/complete-production — 제작 완료 (IN_PRODUCTION/DELAY_REQUESTED → APPROVED_FULFILLMENT_PENDING) */
    @PostMapping("/{id}/complete-production")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse completeProduction(@PathVariable Long id,
                                                      @AuthenticationPrincipal AdminPrincipal admin) {
        OrderProductionUseCase.ProductionResult result = orderProductionUseCase.completeProduction(
                id, admin.adminUserId());
        return OrderProductionResponse.from(result);
    }

    /** PATCH /api/v1/admin/orders/{id}/expected-ship-date — 예상 출고일 설정/갱신 */
    @PatchMapping("/{id}/expected-ship-date")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse setExpectedShipDate(@PathVariable Long id,
                                                       @RequestBody SetExpectedShipDateRequest request) {
        OrderProductionUseCase.ProductionResult result =
                orderProductionUseCase.setExpectedShipDate(id, request.expectedShipDate());
        return OrderProductionResponse.from(result);
    }

    /** POST /api/v1/admin/orders/{id}/delay — 고객 동의 후 배송 지연 상태로 전환 */
    @PostMapping("/{id}/delay")
    @ResponseStatus(HttpStatus.OK)
    public OrderProductionResponse requestDelay(@PathVariable Long id) {
        OrderProductionUseCase.ProductionResult result = orderProductionUseCase.requestDelay(id);
        return OrderProductionResponse.from(result);
    }

    /** POST /api/v1/admin/orders/{id}/cancel-for-delay-rejection — 고객 지연 거절로 취소 */
    @PostMapping("/{id}/cancel-for-delay-rejection")
    @ResponseStatus(HttpStatus.OK)
    public OrderDelayCancellationResponse cancelForDelayRejection(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal admin) {
        OrderProductionUseCase.DelayCancellationResult result =
                orderProductionUseCase.cancelForDelayRejection(id, admin.adminUserId());
        return OrderDelayCancellationResponse.from(result);
    }
}
