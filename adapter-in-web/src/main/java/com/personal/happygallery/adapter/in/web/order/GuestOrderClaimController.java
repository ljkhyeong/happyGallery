package com.personal.happygallery.adapter.in.web.order;

import com.personal.happygallery.adapter.in.web.order.dto.OrderClaimRequest;
import com.personal.happygallery.adapter.in.web.order.dto.OrderClaimResponse;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/claims")
public class GuestOrderClaimController {

    private final OrderClaimUseCase orderClaimUseCase;

    public GuestOrderClaimController(OrderClaimUseCase orderClaimUseCase) {
        this.orderClaimUseCase = orderClaimUseCase;
    }

    @GetMapping
    @Operation(operationId = "listGuestOrderClaims")
    public List<OrderClaimResponse> list(
            @PathVariable Long orderId,
            @RequestHeader("X-Access-Token") String accessToken) {
        return OrderClaimResponse.fromAll(
                orderClaimUseCase.listGuestClaims(orderId, accessToken));
    }

    @PostMapping
    @Operation(operationId = "requestGuestOrderClaim")
    public OrderClaimResponse request(
            @PathVariable Long orderId,
            @RequestHeader("X-Access-Token") String accessToken,
            @Valid @RequestBody OrderClaimRequest request) {
        return OrderClaimResponse.from(orderClaimUseCase.requestGuestClaim(
                orderId, accessToken, request.toCommand()));
    }
}
