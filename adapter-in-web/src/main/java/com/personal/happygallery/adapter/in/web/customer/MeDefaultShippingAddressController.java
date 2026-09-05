package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.DefaultShippingAddressResponse;
import com.personal.happygallery.adapter.in.web.order.dto.UpdateShippingAddressRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.customer.port.in.DefaultShippingAddressUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/default-shipping-address")
public class MeDefaultShippingAddressController {
    private final DefaultShippingAddressUseCase addresses;
    public MeDefaultShippingAddressController(DefaultShippingAddressUseCase addresses) { this.addresses = addresses; }

    @GetMapping
    @Operation(operationId = "getMyDefaultShippingAddress")
    public DefaultShippingAddressResponse get(@AuthenticationPrincipal CustomerPrincipal customer) {
        return DefaultShippingAddressResponse.from(addresses.get(customer.userId()));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "saveMyDefaultShippingAddress")
    public void save(@AuthenticationPrincipal CustomerPrincipal customer, @Valid @RequestBody UpdateShippingAddressRequest request) {
        addresses.save(customer.userId(), request.version(), request.toAddress());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteMyDefaultShippingAddress")
    public void delete(@AuthenticationPrincipal CustomerPrincipal customer, @RequestParam @PositiveOrZero long version) {
        addresses.delete(customer.userId(), version);
    }
}
