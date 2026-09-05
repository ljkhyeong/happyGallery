package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.RestockAlertRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.RestockAlertResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.product.port.in.RestockAlertUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/restock-alerts")
public class MeRestockAlertController {
    private final RestockAlertUseCase alerts;

    public MeRestockAlertController(RestockAlertUseCase alerts) { this.alerts = alerts; }

    @GetMapping
    @Operation(operationId = "listMyRestockAlerts")
    public List<RestockAlertResponse> list(@AuthenticationPrincipal CustomerPrincipal customer) {
        return alerts.list(customer.userId()).stream()
                .map(view -> RestockAlertResponse.from(view.alert(), view.productName())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "registerMyRestockAlert")
    public void register(@AuthenticationPrincipal CustomerPrincipal customer,
                         @Valid @RequestBody RestockAlertRequest request) {
        alerts.register(customer.userId(), request.productId(), request.productVariantId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "cancelMyRestockAlert")
    public void cancel(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable Long id) {
        alerts.cancel(customer.userId(), id);
    }
}
