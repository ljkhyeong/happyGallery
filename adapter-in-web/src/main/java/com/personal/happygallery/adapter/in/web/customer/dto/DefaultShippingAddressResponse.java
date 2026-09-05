package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.DefaultShippingAddressUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

public record DefaultShippingAddressResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) SavedShippingAddress shippingAddress) {
    public record SavedShippingAddress(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String recipientName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phone,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String postalCode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String addressLine1,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String addressLine2) {}

    public static DefaultShippingAddressResponse from(DefaultShippingAddressUseCase.View view) {
        var a = view.shippingAddress();
        return new DefaultShippingAddressResponse(view.version(), a == null ? null : new SavedShippingAddress(
                a.recipientName(), a.phone(), a.postalCode(), a.addressLine1(), a.addressLine2()));
    }
}
