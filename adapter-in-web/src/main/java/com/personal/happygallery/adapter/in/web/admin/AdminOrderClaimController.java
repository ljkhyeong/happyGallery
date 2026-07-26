package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.CompleteOrderExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminOrderClaimPageResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.ResolveOrderClaimRequest;
import com.personal.happygallery.adapter.in.web.order.dto.OrderClaimResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/order-claims")
public class AdminOrderClaimController {

    private final AdminOrderClaimUseCase orderClaimUseCase;

    public AdminOrderClaimController(AdminOrderClaimUseCase orderClaimUseCase) {
        this.orderClaimUseCase = orderClaimUseCase;
    }

    @GetMapping
    @Operation(operationId = "listAdminOrderClaims")
    public AdminOrderClaimPageResponse list(
            @RequestParam(required = false) OrderClaimStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int size) {
        return AdminOrderClaimPageResponse.from(orderClaimUseCase.list(status, cursor, size));
    }

    @PostMapping("/{claimId}/resolve")
    @Operation(operationId = "resolveOrderClaim")
    public OrderClaimResponse resolve(
            @PathVariable Long claimId,
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody ResolveOrderClaimRequest request) {
        return OrderClaimResponse.from(
                orderClaimUseCase.resolve(claimId, admin.adminUserId(), request.toCommand()));
    }

    @PostMapping("/{claimId}/complete-exchange")
    @Operation(operationId = "completeOrderClaimExchange")
    public OrderClaimResponse completeExchange(
            @PathVariable Long claimId,
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody CompleteOrderExchangeRequest request) {
        return OrderClaimResponse.from(
                orderClaimUseCase.completeExchange(
                        claimId, admin.adminUserId(), request.toCommand()));
    }
}
