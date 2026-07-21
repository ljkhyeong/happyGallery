package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.order.dto.OrderCustomerActionResponse;
import com.personal.happygallery.adapter.in.web.order.dto.OrderDelayResponseRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
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

    public MeOrderCustomerActionController(OrderCustomerActionUseCase orderCustomerActionUseCase) {
        this.orderCustomerActionUseCase = orderCustomerActionUseCase;
    }

    @DeleteMapping("/{id}")
    public OrderCustomerActionResponse cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return OrderCustomerActionResponse.from(
                orderCustomerActionUseCase.cancelMemberOrder(id, customer.userId()));
    }

    @PostMapping("/{id}/delay-response")
    public OrderCustomerActionResponse respondToDelay(
            @PathVariable Long id,
            @Valid @RequestBody OrderDelayResponseRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return OrderCustomerActionResponse.from(
                orderCustomerActionUseCase.respondToMemberDelay(
                        id, customer.userId(), request.decision()));
    }
}
