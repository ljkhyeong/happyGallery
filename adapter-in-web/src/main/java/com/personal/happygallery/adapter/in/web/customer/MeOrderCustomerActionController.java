package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.order.dto.OrderCustomerActionResponse;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDelayResponseRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingAddressUseCase;
import com.personal.happygallery.adapter.in.web.order.dto.UpdateShippingAddressRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/orders")
public class MeOrderCustomerActionController {

    private final OrderCustomerActionUseCase orderCustomerActionUseCase;
    private final OrderShippingAddressUseCase shippingAddressUseCase;

    public MeOrderCustomerActionController(OrderCustomerActionUseCase orderCustomerActionUseCase,
            OrderShippingAddressUseCase shippingAddressUseCase) {
        this.orderCustomerActionUseCase = orderCustomerActionUseCase;
        this.shippingAddressUseCase = shippingAddressUseCase;
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "cancelMyOrder")
    public OrderCustomerActionResponse cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return OrderCustomerActionResponse.from(
                orderCustomerActionUseCase.cancelMemberOrder(id, customer.userId()));
    }

    @PostMapping("/{id}/delay-response")
    @Operation(operationId = "respondToMyOrderDelay")
    public OrderCustomerActionResponse respondToDelay(
            @PathVariable Long id,
            @Valid @RequestBody OrderDelayResponseRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return OrderCustomerActionResponse.from(
                orderCustomerActionUseCase.respondToMemberDelay(
                        id, customer.userId(), request.decision()));
    }
    @PutMapping("/{id}/shipping-address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "updateMyOrderShippingAddress")
    public void updateShippingAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomerPrincipal customer,
            @Valid @RequestBody UpdateShippingAddressRequest request) {
        shippingAddressUseCase.updateMember(id, customer.userId(), request.version(), request.toAddress());
    }
}
