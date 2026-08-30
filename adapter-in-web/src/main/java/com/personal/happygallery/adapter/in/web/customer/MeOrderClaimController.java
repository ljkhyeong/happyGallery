package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.order.dto.OrderClaimRequest;
import com.personal.happygallery.adapter.in.web.order.dto.OrderClaimResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/orders/{orderId}/claims")
public class MeOrderClaimController {

    private final OrderClaimUseCase orderClaimUseCase;

    public MeOrderClaimController(OrderClaimUseCase orderClaimUseCase) {
        this.orderClaimUseCase = orderClaimUseCase;
    }

    @GetMapping
    @Operation(operationId = "listMyOrderClaims")
    public List<OrderClaimResponse> list(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return OrderClaimResponse.fromAll(
                orderClaimUseCase.listMemberClaims(orderId, customer.userId()));
    }

    @PostMapping
    @Operation(operationId = "requestMyOrderClaim")
    public OrderClaimResponse request(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomerPrincipal customer,
            @Valid @RequestBody OrderClaimRequest request) {
        return OrderClaimResponse.from(orderClaimUseCase.requestMemberClaim(
                orderId, customer.userId(), request.toCommand()));
    }
}
