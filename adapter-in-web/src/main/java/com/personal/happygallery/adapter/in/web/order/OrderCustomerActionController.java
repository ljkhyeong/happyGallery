package com.personal.happygallery.adapter.in.web.order;

import com.personal.happygallery.adapter.in.web.order.dto.OrderCustomerActionResponse;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDelayResponseRequest;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderCustomerActionController {

    private final OrderCustomerActionUseCase orderCustomerActionUseCase;

    public OrderCustomerActionController(OrderCustomerActionUseCase orderCustomerActionUseCase) {
        this.orderCustomerActionUseCase = orderCustomerActionUseCase;
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "cancelGuestOrder")
    public OrderCustomerActionResponse cancel(
            @PathVariable Long id,
            @RequestHeader("X-Access-Token") String accessToken) {
        return OrderCustomerActionResponse.from(
                orderCustomerActionUseCase.cancelGuestOrder(id, accessToken));
    }

    @PostMapping("/{id}/delay-response")
    @Operation(operationId = "respondToGuestOrderDelay")
    public OrderCustomerActionResponse respondToDelay(
            @PathVariable Long id,
            @RequestHeader("X-Access-Token") String accessToken,
            @Valid @RequestBody OrderDelayResponseRequest request) {
        return OrderCustomerActionResponse.from(
                orderCustomerActionUseCase.respondToGuestDelay(
                        id, accessToken, request.decision()));
    }
}
